package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.FraudTypeResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.response.*;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections;
import java.time.LocalDateTime;
import java.util.HashMap;
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
     * @param housingCategory 건축물 유형. checklist_items.category ENUM과 이름이 일치해야
     *                        체크리스트 생성 시 유형별 항목이 붙는다.
     *                        (SINGLE_FAMILY / MULTI_FAMILY / APARTMENT / MULTI_HOUSEHOLD / OFFICETEL)
     * @param trustProperty   신탁주택 여부. TRUE면 체크리스트에 TRUST_PROPERTY 항목이 추가된다.
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
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put("requestId", requestId);
        params.put("roadAddress", roadAddress);
        params.put("detailAddress", detailAddress);
        params.put("deposit", deposit);
        params.put("result", evalResult.getOverallRiskLevel());
        params.put("housingCategory", housingCategory);
        params.put("trustProperty", trustProperty);
        reportMapper.insertReportMap(params);
        return Long.valueOf(params.get("reportId").toString());
    }

    private List<CheckResultView> insertCheckResults(Long reportId, List<CheckResult> results) {
        return results.stream().map(c -> {
            Map<String, Object> params = new HashMap<>();
            params.put("reportId", reportId);
            params.put("checkType", c.getCheckType());
            params.put("riskLevel", c.getRiskLevel());
            params.put("dataStatus", c.getDataStatus());
            params.put("evidenceJson", toJson(c.getEvidence()));

            reportMapper.insertCheckResult(params);

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
            Map<String, Object> params = new HashMap<>();
            params.put("reportId", reportId);
            params.put("fraudType", f.getFraudType());
            params.put("riskLevel", f.getRiskLevel());
            reportMapper.insertFraudTypeMap(params);
            Long fraudTypeId = Long.valueOf(params.get("fraudTypeId").toString());

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
