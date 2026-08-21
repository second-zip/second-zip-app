package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.domain.ReportFraudType;
import com.secondzip.backend.report.domain.ReportSpecialTerm;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.response.*;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        AnalysisReport report = reportMapper.findReportById(reportId);
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
                report.getAnalysisReportId(),
                report.getRoadAddress(),
                report.getDetailAddress(),
                report.getDeposit(),
                report.getRiskLevel(),
                report.getFavorite(),
                // 이 필드가 추가되기 전에 저장된 리포트는 값이 없다. null 그대로 내려보낸다.
                report.getHousingCategory(),
                report.getTrustProperty(),
                checkViews,
                fraudViews,
                specialTermViews
        );
    }

    // 필수 점검 항목 조회
    private List<CheckResultView> buildCheckResultViews(Long reportId) {
        List<ReportCheckResult> rows = reportMapper.findCheckResultsByReportId(reportId);
        return rows.stream()
                .map(row -> new CheckResultView(
                        row.getCheckType(),
                        row.getRiskLevel(),
                        parseDataStatus(row.getDataStatus()),
                        parseEvidence(row.getEvidence())
                ))
                .collect(Collectors.toList());
    }

    private DataStatus parseDataStatus(String raw) {
        if (raw == null) {
            return DataStatus.VERIFIED;
        }
        try {
            return DataStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return DataStatus.VERIFIED;
        }
    }

    // 사기 유형 조회
    private List<FraudTypeView> buildFraudTypeViews(Long reportId) {
        List<ReportFraudType> fraudTypeRows = reportMapper.findFraudTypesByReportId(reportId);
        return fraudTypeRows.stream().map(row -> {
            List<DetailResult> details =
                    reportMapper.findDetailResultsByFraudTypeId(row.getReportFraudTypeId());

            List<DetailResultView> detailViews = details.stream()
                    .map(d -> new DetailResultView(
                            d.getDetailType(),
                            d.getRiskLevel(),
                            d.getDataStatus() != null
                                    ? d.getDataStatus()
                                    : DataStatus.VERIFIED
                    ))
                    .collect(Collectors.toList());

            return new FraudTypeView(row.getFraudType(), row.getRiskLevel(), detailViews);
        }).collect(Collectors.toList());
    }

    // AI 추천 특약 조회
    private List<SpecialTermView> buildSpecialTermViews(Long reportId) {
        List<ReportSpecialTerm> rows =
                reportMapper.findSpecialTermsByReportId(reportId);

        // 순번은 테이블에 없다. 조회 순서대로 1부터 붙인다.
        return IntStream.range(0, rows.size())
                .mapToObj(index -> {
                    ReportSpecialTerm row = rows.get(index);

                    return new SpecialTermView(
                            index + 1,
                            row.getTitle(),
                            row.getContent()
                    );
                })
                .collect(Collectors.toList());
    }

    // evidence JSON 파싱
    private Map<String, Object> parseEvidence(String evidenceRaw) {
        if (evidenceRaw == null) return Collections.emptyMap();
        try {
            return objectMapper.readValue(evidenceRaw, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("evidence JSON 파싱 실패: {}", evidenceRaw, e);
            return Collections.emptyMap();
        }
    }
}
