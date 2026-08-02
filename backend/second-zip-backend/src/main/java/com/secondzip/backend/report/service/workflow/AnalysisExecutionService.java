package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
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
import com.secondzip.backend.report.service.external.client.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            BuildingRegisterAnalysisData buildingRegister =
                    buildingRegisterDataParser.parse(
                            state.getRequiredDocuments(),
                            state.getBuildingRegisterData(),
                            state.getBuildingType(),
                            state.getBuildingUse()
                    );
            if (!Boolean.TRUE.equals(
                    buildingRegister.getBuildingData()
                            .getIllegalBuildingVerified()
            )) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "위반건축물 여부를 확인하지 못해 분석을 완료할 수 없습니다."
                );
            }
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
            PriceData price = priceDataProvider.getPriceData(
                    state.getTarget(),
                    state.getBuildingType()
            );
            price = mergeOfficialPrice(
                    price,
                    buildingRegister.getOfficialPrice()
            );
            if (!hasPriceBasis(price)) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "실거래가 또는 공시가격을 확인하지 못해 분석을 완료할 수 없습니다."
                );
            }

            RiskEvaluationResult evaluation =
                    riskEvaluationService.evaluate(
                            registry,
                            buildingRegister.getBuildingData(),
                            price,
                            state.getDeposit(),
                            state.getRoadAddress()
                    );
            ReportDetailResponse report = reportPersistenceService.save(
                    accountId,
                    requestId,
                    state.getRoadAddress(),
                    state.getDetailAddress(),
                    state.getDeposit(),
                    evaluation
            );
            state.setReportId(report.getAnalysisReportId());
            state.setStatus(AnalysisRequestStatus.COMPLETED);
            state.setFailureMessage(null);
            state.setBuildingRegisterData(new java.util.LinkedHashMap<>());
            workflowStore.save(state);
            return report;
        } catch (RuntimeException e) {
            state.setStatus(AnalysisRequestStatus.FAILED);
            state.setFailureMessage(
                    "외부 데이터 조회 또는 리포트 저장 중 오류가 발생했습니다."
            );
            workflowStore.save(state);
            throw e;
        }
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

    private boolean hasPriceBasis(PriceData price) {
        return price != null
                && (price.getRecentSalePrice() != null
                || price.getOfficialPrice() != null);
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
