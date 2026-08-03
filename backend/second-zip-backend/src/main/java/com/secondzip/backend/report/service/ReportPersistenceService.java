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
    @Transactional
    public ReportDetailResponse save(Long accountId, String requestId,
                                     String roadAddress, String detailAddress,
                                     Long deposit, RiskEvaluationResult evalResult) {
        // 보고서 만들고 저장 -> 그 보고서의 ID를 반환
        Long reportId = insertReport(
                accountId,
                requestId,
                roadAddress,
                detailAddress,
                deposit,
                evalResult
        );
        // 해당 보고서에 필수 점검 결과 저장
        List<CheckResultView> checkViews = insertCheckResults(reportId, evalResult.getCheckResults());
        // 해당 보고서에 사기 유형과 각 유형의 판단 결과 저장
        List<FraudTypeView> fraudViews = insertFraudTypes(reportId, evalResult.getFraudTypeResults());

        return new ReportDetailResponse(
                reportId, roadAddress, detailAddress, deposit,
                evalResult.getOverallRiskLevel(), false, checkViews, fraudViews
        );
    }

    private Long insertReport(Long accountId, String requestId,
                              String roadAddress, String detailAddress,
                              Long deposit, RiskEvaluationResult evalResult) {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put("requestId", requestId);
        params.put("roadAddress", roadAddress);
        params.put("detailAddress", detailAddress);
        params.put("deposit", deposit);
        params.put("result", evalResult.getOverallRiskLevel());
        reportMapper.insertReportMap(params);
        return Long.valueOf(params.get("reportId").toString());
    }

    private List<CheckResultView> insertCheckResults(Long reportId, List<CheckResult> results) {
        return results.stream().map(c -> {
            Map<String, Object> params = new HashMap<>();
            params.put("reportId", reportId);
            params.put("checkType", c.getCheckType());
            params.put("riskLevel", c.getRiskLevel());
            params.put("evidenceJson", toJson(c.getEvidence()));

            reportMapper.insertCheckResult(params);

            return new CheckResultView(c.getCheckType(), c.getRiskLevel(), c.getEvidence());
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
                reportMapper.insertDetailResult(fraudTypeId, d.getDetailType(), d.getRiskLevel());
                return new DetailResultView(d.getDetailType(), d.getRiskLevel());
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
}
