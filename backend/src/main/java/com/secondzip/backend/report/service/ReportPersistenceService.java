package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.domain.ReportFraudType;
import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.FraudTypeResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.VerifiedChecklistItem;
import com.secondzip.backend.report.dto.response.*;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReportPersistenceService {

    private final ReportMapper reportMapper;
    private final ObjectMapper objectMapper;    // JSON 변환용
    private final ReportQueryService reportQueryService;

    // 작성, 수정, 삭제, 저장 담당
    /**
     * 건축물 유형. checklist_items.category ENUM과 이름이 일치해야
     *                        체크리스트 생성 시 유형별 항목이 붙음.
     *                        (SINGLE_FAMILY / MULTI_FAMILY / APARTMENT / MULTI_HOUSEHOLD / OFFICETEL)
     * 신탁주택 여부. TRUE면 체크리스트에 TRUST_PROPERTY 항목이 추가.
     */
    @Transactional
    public ReportDetailResponse save(Long accountId, String requestId,
                                     String roadAddress, String detailAddress,
                                     Long deposit, RiskEvaluationResult evalResult,
                                     String housingCategory, boolean trustProperty) {
        // 보고서 만들고 저장 -> 그 보고서의 ID를 반환
        Long reportId = insertReport(
                accountId,
                requestId,
                roadAddress,
                detailAddress,
                deposit,
                evalResult,
                housingCategory,
                trustProperty
        );
        // 해당 보고서에 필수 점검 결과 저장
        List<CheckResultView> checkViews = insertCheckResults(reportId, evalResult.getCheckResults());
        // 해당 보고서에 사기 유형과 각 유형의 판단 결과 저장
        List<FraudTypeView> fraudViews = insertFraudTypes(reportId, evalResult.getFraudTypeResults());
        // 분석에서 안전(VERIFIED + SAFE)으로 판정된 항목을 체크리스트에 넘겨준다
        insertChecklistVerifications(reportId, evalResult);

        return new ReportDetailResponse(
                reportId, roadAddress, detailAddress, deposit,
                evalResult.getOverallRiskLevel(), false,
                housingCategory, trustProperty,
                checkViews, fraudViews, Collections.emptyList()
        );
    }

    private Long insertReport(Long accountId, String requestId,
                              String roadAddress, String detailAddress,
                              Long deposit, RiskEvaluationResult evalResult,
                              String housingCategory, boolean trustProperty) {
        AnalysisReport report = AnalysisReport.builder()
                .accountId(accountId)
                .requestId(requestId)
                .roadAddress(roadAddress)
                .detailAddress(detailAddress)
                .deposit(deposit)
                .riskLevel(evalResult.getOverallRiskLevel())
                .housingCategory(housingCategory)
                .trustProperty(trustProperty)
                .build();

        // useGeneratedKeys 가 report.analysisReportId 를 채운다.
        reportMapper.insertReport(report);
        return report.getAnalysisReportId();
    }

    private List<CheckResultView> insertCheckResults(Long reportId, List<CheckResult> results) {
        return results.stream().map(c -> {
            reportMapper.insertCheckResult(
                    ReportCheckResult.builder()
                            .analysisReportId(reportId)
                            .checkType(c.getCheckType())
                            .riskLevel(c.getRiskLevel())
                            .dataStatus(nameOf(c.getDataStatus()))
                            .evidence(toJson(c.getEvidence()))
                            .build()
            );

            return new CheckResultView(
                    c.getCheckType(),
                    c.getRiskLevel(),
                    c.getDataStatus(),
                    c.getEvidence()
            );
        }).collect(Collectors.toList());
    }

    private List<FraudTypeView> insertFraudTypes(Long reportId, List<FraudTypeResult> fraudResults) {
        return fraudResults.stream().map(f -> {
            ReportFraudType fraudTypeRow = ReportFraudType.builder()
                    .analysisReportId(reportId)
                    .fraudType(f.getFraudType())
                    .riskLevel(f.getRiskLevel())
                    .build();

            // useGeneratedKeys 가 fraudTypeRow.reportFraudTypeId 를 채운다.
            reportMapper.insertFraudType(fraudTypeRow);
            Long fraudTypeId = fraudTypeRow.getReportFraudTypeId();

            List<DetailResultView> details = f.getDetails().stream().map(d -> {
                reportMapper.insertDetailResult(
                        fraudTypeId,
                        d.getDetailType(),
                        d.getRiskLevel(),
                        d.getDataStatus()
                );
                return new DetailResultView(
                        d.getDetailType(),
                        d.getRiskLevel(),
                        d.getDataStatus()
                );
            }).collect(Collectors.toList());

            return new FraudTypeView(f.getFraudType(), f.getRiskLevel(), details);
        }).collect(Collectors.toList());
    }

    /**
     * 분석 판정이 안전(VERIFIED + SAFE)으로 나온 체크리스트 항목을 저장.
     * 사용자가 나중에 이 리포트로 체크리스트를 만들면
     * ReportChecklistMapper.insertChecklistItems 가 이 행들을 LEFT JOIN 해서
     * 해당 항목을 처음부터 체크된 상태로 생성.
     */
    private void insertChecklistVerifications(Long reportId, RiskEvaluationResult evalResult) {
        List<VerifiedChecklistItem> verified =
                ChecklistAutoCheckResolver.resolve(evalResult);

        if (verified.isEmpty()) {
            log.debug("리포트 {} - 자동 체크할 체크리스트 항목 없음", reportId);
            return;
        }

        reportMapper.insertChecklistVerifications(reportId, verified);
        log.debug("리포트 {} - 체크리스트 자동 체크 {}건", reportId, verified.size());
    }

    /**
     * data_status 컬럼에 넣을 문자열.
     * ReportCheckResult.dataStatus 가 String 인 이유는 그 클래스 주석 참고.
     */
    private String nameOf(DataStatus dataStatus) {
        return dataStatus != null ? dataStatus.name() : null;
    }

    // 필수 점검 판단 데이터 JSON으로 변경
    private String toJson(Map<String, Object> evidence){
        try{
            return objectMapper.writeValueAsString(evidence);
        }catch (JsonProcessingException e){
            log.error("evidence JSON 변환 실패", e);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "리포트 판단 근거를 저장하지 못했습니다."
            );
        }
    }

    // 리포트 삭제
    @Transactional
    public void deleteReport(Long accountId, Long reportId) {
        reportQueryService.validateOwnership(accountId, reportId);
        reportMapper.deleteReport(reportId);
    }

    // 즐겨찾기 추가
    @Transactional
    public void addFavorite(Long accountId, Long reportId) {
        reportQueryService.validateOwnership(accountId, reportId);
        reportMapper.updateFavorite(reportId, true, LocalDateTime.now());
    }

    // 즐겨찾기 해제
    @Transactional
    public void removeFavorite(Long accountId, Long reportId) {
        reportQueryService.validateOwnership(accountId, reportId);
        reportMapper.updateFavorite(reportId, false, null);
    }
}
