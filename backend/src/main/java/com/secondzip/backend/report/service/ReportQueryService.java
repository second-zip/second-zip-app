package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.response.*;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
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

    // 조회 담당

    // === 리포트 목록 조회 ===
    public ReportListResponse getReportList(Long accountId) {
        List<ReportListItem> reports = reportMapper.findReportsByAccountId(accountId);
        int totalCount = reportMapper.countReportsByAccountId(accountId);
        return new ReportListResponse(reports, totalCount);
    }

    // === 리포트 상세 조회 ===

    public Long findReportIdByRequestId(Long accountId, String requestId) {
        return reportMapper.findReportIdByRequestId(accountId, requestId);
    }

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
        // validateOwnership 통과 후 조회 전에 다른 요청이 리포트를 지울 수 있다.
        // 그 경우 아래에서 NPE가 나 500으로 나가므로 여기서 404로 정리한다.
        if (report == null) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "리포트를 찾을 수 없습니다."
            );
        }

        List<CheckResultView> checkViews = buildCheckResultViews(reportId);
        List<FraudTypeView> fraudViews = buildFraudTypeViews(reportId);

        //특약
        List<SpecialTermView> specialTermViews = buildSpecialTermViews(reportId);

        return new ReportDetailResponse(
                (Long) report.get("analysisReportId"),
                (String) report.get("roadAddress"),
                (String) report.get("detailAddress"),
                (Long) report.get("deposit"),
                RiskLevel.valueOf(report.get("result").toString()),
                (Boolean) report.get("favorite"),
                // 이 필드가 추가되기 전에 저장된 리포트는 값이 없다. null 그대로 내려보낸다.
                (String) report.get("housingCategory"),
                (Boolean) report.get("trustProperty"),
                checkViews,
                fraudViews,
                specialTermViews
        );
    }

    // 필수 점검 항목 조회
    private List<CheckResultView> buildCheckResultViews(Long reportId) {
        List<Map<String, Object>> rows = reportMapper.findCheckResultsByReportId(reportId);
        return rows.stream().map(row -> {
            CheckType checkType = CheckType.valueOf(row.get("checkType").toString());
            RiskLevel riskLevel = RiskLevel.valueOf(row.get("riskLevel").toString());
            Map<String, Object> evidence = parseEvidence(row.get("evidence"));
            return new CheckResultView(
                    checkType,
                    riskLevel,
                    parseDataStatus(row.get("dataStatus")),
                    evidence
            );
        }).collect(Collectors.toList());
    }

    /** V3 이전에 저장된 행은 data_status가 없을 수 있어 VERIFIED로 본다. */
    private DataStatus parseDataStatus(Object raw) {
        if (raw == null) {
            return DataStatus.VERIFIED;
        }
        try {
            return DataStatus.valueOf(raw.toString());
        } catch (IllegalArgumentException e) {
            return DataStatus.VERIFIED;
        }
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
                    .map(d -> new DetailResultView(
                            d.getDetailType(),
                            d.getRiskLevel(),
                            d.getDataStatus() != null
                                    ? d.getDataStatus()
                                    : DataStatus.VERIFIED
                    ))
                    .collect(Collectors.toList());

            return new FraudTypeView(fraudType, riskLevel, detailViews);
        }).collect(Collectors.toList());
    }

    // AI 추천 특약 조회
    private List<SpecialTermView> buildSpecialTermViews(Long reportId) {
        List<Map<String, Object>> rows =
                reportMapper.findSpecialTermsByReportId(reportId);

        return java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    Map<String, Object> row = rows.get(index);

                    return new SpecialTermView(
                            index + 1,
                            (String) row.get("title"),
                            (String) row.get("content")
                    );
                })
                .collect(Collectors.toList());
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


