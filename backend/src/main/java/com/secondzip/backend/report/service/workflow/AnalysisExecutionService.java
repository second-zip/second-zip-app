package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.external.BuildingRegisterAnalysisData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.service.ReportPersistenceService;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.RiskEvaluationService;
import com.secondzip.backend.report.service.TrustPropertyResolver;
import com.secondzip.backend.report.service.external.client.*;
import com.secondzip.backend.report.service.SpecialTermService;
import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisExecutionService {

    private final AnalysisWorkflowStore workflowStore;
    private final BuildingRegisterDataParser buildingRegisterDataParser;
    private final RegistryDataProvider registryDataProvider;
    private final PriceDataProvider priceDataProvider;
    private final RiskEvaluationService riskEvaluationService;
    private final ReportPersistenceService reportPersistenceService;
    private final ReportQueryService reportQueryService;
    private final SpecialTermService specialTermService;
    private final TrustPropertyResolver trustPropertyResolver;

    public ReportDetailResponse execute(
            Long accountId,
            String requestId
    ) {
        String lockToken = acquireLock(requestId);
        try {
            return executeLocked(accountId, requestId);
        } finally {
            workflowStore.releaseExecutionLock(requestId, lockToken);
        }
    }

    public ReportDetailResponse retry(
            Long accountId,
            String requestId
    ) {
        String lockToken = acquireLock(requestId);
        try {
            AnalysisWorkflowState state =
                    workflowStore.findOwned(requestId, accountId);
            if (state.getStatus() != AnalysisRequestStatus.FAILED) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "실패한 분석 요청만 재시도할 수 있습니다."
                );
            }
            if (state.getRequiredDocuments() == null
                    || state.getCompletedDocuments() == null
                    || !state.getCompletedDocuments().containsAll(
                    state.getRequiredDocuments()
            )) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "건축물대장 발급이 완료되지 않아 인증부터 다시 진행해야 합니다."
                );
            }
            state.setStatus(AnalysisRequestStatus.PROCESSING);
            state.setFailureMessage(null);
            workflowStore.save(state);
            return executeLocked(accountId, requestId);
        } finally {
            workflowStore.releaseExecutionLock(requestId, lockToken);
        }
    }

    private ReportDetailResponse executeLocked(
            Long accountId,
            String requestId
    ) {
        AnalysisWorkflowState state =
                workflowStore.findOwned(requestId, accountId);
        Long existingReportId = reportQueryService.findReportIdByRequestId(
                accountId,
                requestId
        );
        if (existingReportId != null) {
            state.setReportId(existingReportId);
            state.setStatus(AnalysisRequestStatus.COMPLETED);
            state.setFailureMessage(null);
            state.setBuildingRegisterData(new java.util.LinkedHashMap<>());
            workflowStore.save(state);
            return reportQueryService.getReportDetail(
                    accountId,
                    existingReportId
            );
        }
        if (state.getStatus() == AnalysisRequestStatus.COMPLETED
                && state.getReportId() != null) {
            return reportQueryService.getReportDetail(
                    accountId,
                    state.getReportId()
            );
        }
        if (state.getStatus() != AnalysisRequestStatus.PROCESSING) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "건축물대장 인증과 발급이 모두 끝난 뒤 분석할 수 있습니다."
            );
        }
        try {
            // ===== 1단계. 무료 데이터 수집 =====
            // 등기부등본은 건당 700원 차감되므로,
            // 돈이 들지 않는 데이터를 먼저 처리.
            BuildingRegisterAnalysisData buildingRegister =
                    buildingRegisterDataParser.parse(
                            state.getRequiredDocuments(),
                            state.getBuildingRegisterData(),
                            state.getBuildingType(),
                            state.getBuildingUse()
                    );
            PriceData price = mergeOfficialPrice(
                    priceDataProvider.getPriceData(
                            state.getTarget(),
                            state.getBuildingType()
                    ),
                    buildingRegister.getOfficialPrice()
            );

            // ===== 2단계. 유료 조회 전 관문 =====
            verifyFreeDataBeforePaidLookup(buildingRegister, price);

            // ===== 3단계. 유료 데이터 조회 (등기부등본) =====
            // 과금 발생. 반드시 위 관문을 통과한 뒤에만 호출할 것.
            RegistryData registry =
                    registryDataProvider.getRegistryDataForAnalysis(
                            state.getTarget(),
                            state.getDetailAddress(),
                            state.getBuildingType()
                    );
            if (registry == null) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "등기부등본 데이터를 확인하지 못해 분석을 완료할 수 없습니다."
                );
            }

            // ===== 4단계. 위험도 평가 및 저장 =====
            RiskEvaluationResult evaluation =
                    riskEvaluationService.evaluate(
                            registry,
                            buildingRegister.getBuildingData(),
                            price,
                            state.getDeposit(),
                            regionAddress(state)
                    );
            // 체크리스트 생성 조건이 되는 두 값을 리포트에 함께 저장한다.
            //
            // housingCategory는 분석 준비 단계에서 이미 확정된 buildingType을 그대로 쓴다.
            // BuildingRegisterDocumentSelector가 5종(단독/다가구/아파트/다세대/오피스텔)
            // 이외의 값을 거부하므로 이 지점에서는 항상 유효하고, 값이 곧
            // checklist_items.category ENUM과 일치한다.
            ReportDetailResponse report = reportPersistenceService.save(
                    accountId,
                    requestId,
                    state.getRoadAddress(),
                    state.getDetailAddress(),
                    state.getDeposit(),
                    evaluation,
                    state.getBuildingType(),
                    trustPropertyResolver.resolve(registry)
            );

            // 리포트 저장이 완료됐으므로 분석 자체는 성공 처리.
            //
            // 이 지점부터는 유료 조회 비용을 이미 지불했고 결과도 DB에 존재.
            // 워크플로 상태 갱신이 실패하더라도(Redis 장애, TTL 만료 등)
            // 절대 예외를 밖으로 던지지 않음. 던지면 사용자는 돈만 쓰고
            // 500 에러를 받으며, 리포트를 목록에서 직접 찾아야 한다.
            state.setReportId(report.getAnalysisReportId());
            state.setStatus(AnalysisRequestStatus.COMPLETED);
            state.setFailureMessage(null);
            state.setBuildingRegisterData(new java.util.LinkedHashMap<>());
            saveStateQuietly(state, "완료 상태");

            // AI 특약은 부가 기능이므로 실패해도 분석 성공에는 영향을 주지 않음
            try {
                specialTermService.generateAndSave(accountId, report.getAnalysisReportId());

                // 생성된 특약까지 포함해서 다시 조회
                return reportQueryService.getReportDetail(accountId, report.getAnalysisReportId());

            } catch (RuntimeException e) {
                log.warn(
                        "AI 특약 자동 생성 실패. reportId={}, message={}",
                        report.getAnalysisReportId(),
                        e.getMessage(),
                        e
                );

                // 특약 생성에 실패해도 기존 분석 결과는 정상 반환
                return report;
            }
        } catch (RuntimeException e) {
            state.setStatus(AnalysisRequestStatus.FAILED);
            state.setFailureMessage(failureMessageOf(e));
            saveStateQuietly(state, "실패 상태");
            throw e;
        }
    }

    /**
     * 실패 사유를 그대로 보존한다.
     *
     * <p>예전에는 모든 실패를 한 문장으로 덮어써서, 등기부등본 실패인지 실거래가 실패인지
     * 위반건축물 실패인지 사용자도 로그도 구분할 수 없었다.
     * {@link BusinessException}은 이미 구체적인 메시지를 담고 있으므로 그것을 쓴다.
     */
    private String failureMessageOf(RuntimeException e) {
        if (e instanceof BusinessException && e.getMessage() != null
                && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return "외부 데이터 조회 또는 리포트 저장 중 오류가 발생했습니다.";
    }

    /**
     * 워크플로 상태 저장 실패가 본래 흐름을 덮어쓰지 않게 한다.
     *
     * <p>성공 경로에서는 이미 확보한 리포트를 잃지 않기 위해,
     * 실패 경로에서는 원래 예외가 저장 실패 예외로 바뀌지 않게 하기 위해 필요하다.
     */
    private void saveStateQuietly(AnalysisWorkflowState state, String what) {
        try {
            workflowStore.save(state);
        } catch (RuntimeException e) {
            log.warn(
                    "분석 워크플로 {} 저장 실패. requestId={}, type={}, message={}",
                    what,
                    state.getRequestId(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    /**
     * 유료 조회(등기부등본) 직전 관문.
     *
     * <p>무료 데이터만으로 판정할 수 있는 실패는 전부 여기서 걸러낸다.
     * 등기부등본은 열람 건당 전자민원캐시가 차감되므로, 어차피 실패할 분석에
     * 비용을 쓰지 않기 위한 장치다.
     *
     * <p><b>무료 외부 데이터가 새로 추가되면</b> 수집은 이 메서드를 호출하기 전에,
     * 검증은 이 메서드 안에 추가해야 한다. 그래야 유료 호출이 항상 마지막에 남는다.
     */
    private void verifyFreeDataBeforePaidLookup(
            BuildingRegisterAnalysisData buildingRegister,
            PriceData price
    ) {
        if (!Boolean.TRUE.equals(
                buildingRegister.getBuildingData()
                        .getIllegalBuildingVerified()
        )) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "위반건축물 여부를 확인하지 못해 분석을 완료할 수 없습니다."
            );
        }
        if (!hasPriceBasis(price)) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "실거래가 또는 공시가격을 확인하지 못해 분석을 완료할 수 없습니다."
            );
        }
    }

    /**
     * 수도권/비수도권 판정에 쓸 주소.
     *
     * <p>반드시 <b>표준화된</b> 도로명주소를 쓴다. 사용자가 입력한 원본은 건물명이나
     * 지번일 수 있어 시도명으로 시작하지 않을 수 있고, 그러면 수도권 매물이
     * 비수도권으로 분류되어 HUG 보증금 한도가 7억이 아닌 5억으로 계산된다.
     * 그 결과 정상 매물이 DANGER로 판정된다.
     */
    private String regionAddress(AnalysisWorkflowState state) {
        AnalysisTarget target = state.getTarget();
        if (target != null
                && target.roadAddress() != null
                && !target.roadAddress().isBlank()) {
            return target.roadAddress();
        }
        return state.getRoadAddress();
    }

    private PriceData mergeOfficialPrice(
            PriceData price,
            Long buildingRegisterOfficialPrice
    ) {
        PriceData merged = price == null ? new PriceData() : price;
        if (merged.getOfficialPrice() == null) {
            merged.setOfficialPrice(buildingRegisterOfficialPrice);
        }
        return merged;
    }

    /** 0 이하는 파싱 실패로 본다. 판정 단계의 pickBasePrice와 기준을 맞춘다. */
    private boolean hasPriceBasis(PriceData price) {
        return price != null
                && (isPositive(price.getRecentSalePrice())
                || isPositive(price.getOfficialPrice()));
    }

    private boolean isPositive(Long value) {
        return value != null && value > 0L;
    }

    private String acquireLock(String requestId) {
        String lockToken = workflowStore.tryAcquireExecutionLock(requestId);
        if (lockToken == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "같은 분석 요청이 이미 처리 중입니다."
            );
        }
        return lockToken;
    }
}
