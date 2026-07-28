package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.response.*;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.FraudType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReportQueryService {

    private final ReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    // 리포트 담당

    // === 리포트 목록 조회 ===
    public ReportListResponse getReportList(Long accountId) {
        List<ReportListItem> reports = reportMapper.findReportsByAccountId(accountId);
        int totalCount = reportMapper.countReportsByAccountId(accountId);
        boolean hasMore = totalCount > reports.size();
        return new ReportListResponse(reports, totalCount, hasMore);
    }

    // === 리포트 상세 조회 ===

    // 리포트 소유자 확인
    public void validateOwnership(Long accountId, Long reportId) {
        Long ownerId = reportMapper.findAccountIdByReportId(reportId);
        if (ownerId == null || !ownerId.equals(accountId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "리포트를 찾을 수 없습니다.");
        }
    }

    // 리포트 상세 조회
    public ReportDetailResponse getReportDetail(Long accountId, Long reportId) {
        validateOwnership(accountId, reportId);

        Map<String, Object> report = reportMapper.findReportById(reportId);

        List<CheckResultView> checkViews = buildCheckResultViews(reportId);
        List<FraudTypeView> fraudViews = buildFraudTypeViews(reportId);

        return new ReportDetailResponse(
                (Long) report.get("analysisReportId"),
                (String) report.get("roadAddress"),
                (String) report.get("detailAddress"),
                (Long) report.get("deposit"),
                RiskLevel.valueOf(report.get("result").toString()),
                (Boolean) report.get("favorite"),
                checkViews,
                fraudViews
        );
    }

    // 필수 점검 항목 조회
    private List<CheckResultView> buildCheckResultViews(Long reportId) {
        List<Map<String, Object>> rows = reportMapper.findCheckResultsByReportId(reportId);
        return rows.stream().map(row -> {
            CheckType checkType = CheckType.valueOf(row.get("checkType").toString());
            RiskLevel riskLevel = RiskLevel.valueOf(row.get("riskLevel").toString());
            Map<String, Object> evidence = parseEvidence(row.get("evidence"));
            return new CheckResultView(checkType, riskLevel, evidence);
        }).collect(Collectors.toList());
    }

    // 사기 유형 조회
    private List<FraudTypeView> buildFraudTypeViews(Long reportId) {
        List<Map<String, Object>> fraudTypeRows = reportMapper.findFraudTypesByReportId(reportId);
        return fraudTypeRows.stream().map(row -> {
            Long fraudTypeId = (Long) row.get("reportFraudTypeId");
            FraudType fraudType = FraudType.valueOf(row.get("fraudType").toString());
            RiskLevel riskLevel = RiskLevel.valueOf(row.get("riskLevel").toString());

            List<DetailResult> details = reportMapper.findDetailResultsByFraudTypeId(fraudTypeId);
            List<DetailResultView> detailViews = details.stream()
                    .map(d -> new DetailResultView(d.getDetailType(), d.getRiskLevel()))
                    .collect(Collectors.toList());

            return new FraudTypeView(fraudType, riskLevel, detailViews);
        }).collect(Collectors.toList());
    }

    // evidence JSON 파싱
    private Map<String, Object> parseEvidence(Object evidenceRaw) {
        if (evidenceRaw == null) return Collections.emptyMap();
        try {
            return objectMapper.readValue(evidenceRaw.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("evidence JSON 파싱 실패: {}", evidenceRaw, e);
            return Collections.emptyMap();
        }
    }
}


