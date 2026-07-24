package com.secondzip.backend.report.service;

import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.FraudTypeResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.response.*;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportPersistenceService {

    private final ReportMapper reportMapper;

    @Transactional
    public ReportDetailResponse save(Long accountId, String roadAddress, String detailAddress,
                                     Long deposit, RiskEvaluationResult evalResult) {
        // 보고서 만들고 저장 -> 그 보고서의 ID를 반환
        Long reportId = insertReport(accountId, roadAddress, detailAddress, deposit, evalResult);
        // 해당 보고서에 필수 점검 결과 저장
        List<CheckResultView> checkViews = insertCheckResults(reportId, evalResult.getCheckResults());
        // 해당 보고서에 사기 유형과 각 유형의 판단 결과 저장
        List<FraudTypeView> fraudViews = insertFraudTypes(reportId, evalResult.getFraudTypeResults());

        return new ReportDetailResponse(
                reportId, roadAddress, detailAddress, deposit,
                evalResult.getOverallRiskLevel(), false, checkViews, fraudViews
        );
    }

    private Long insertReport(Long accountId, String roadAddress, String detailAddress,
                              Long deposit, RiskEvaluationResult evalResult) {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put("roadAddress", roadAddress);
        params.put("detailAddress", detailAddress);
        params.put("deposit", deposit);
        params.put("result", evalResult.getOverallRiskLevel());
        reportMapper.insertReportMap(params);
        return Long.valueOf(params.get("reportId").toString());
    }

    private List<CheckResultView> insertCheckResults(Long reportId, List<CheckResult> results) {
        return results.stream().map(c -> {
            reportMapper.insertCheckResult(reportId, c.getCheckType(), c.getRiskLevel());
            return new CheckResultView(c.getCheckType(), c.getRiskLevel());
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
}