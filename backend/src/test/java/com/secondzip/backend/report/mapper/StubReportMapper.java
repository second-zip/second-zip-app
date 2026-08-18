package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.VerifiedChecklistItem;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 ReportMapper 기본 구현.
 * 모든 메서드가 아무 동작도 하지 않으며, 각 테스트는 필요한 것만 오버라이드한다.
 * ReportMapper에 메서드가 추가되면 이 파일만 수정하면 된다.
 */
public class StubReportMapper implements ReportMapper {

    @Override
    public void insertCheckResult(Map<String, Object> params) {
    }

    @Override
    public void insertFraudTypeMap(Map<String, Object> params) {
    }

    @Override
    public void insertDetailResult(
            Long fraudTypeId,
            DetailType detailType,
            RiskLevel riskLevel,
            DataStatus dataStatus
    ) {
    }

    @Override
    public void insertReportMap(Map<String, Object> params) {
    }

    @Override
    public void insertChecklistVerifications(
            Long analysisReportId,
            List<VerifiedChecklistItem> items
    ) {
    }

    @Override
    public List<ReportListItem> findReportsByAccountId(Long accountId) {
        return List.of();
    }

    @Override
    public int countReportsByAccountId(Long accountId) {
        return 0;
    }

    @Override
    public Long findReportIdByRequestId(Long accountId, String requestId) {
        return null;
    }

    @Override
    public Long findAccountIdByReportId(Long reportId) {
        return null;
    }

    @Override
    public Map<String, Object> findReportById(Long reportId) {
        return Map.of();
    }

    @Override
    public List<Map<String, Object>> findCheckResultsByReportId(Long reportId) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> findFraudTypesByReportId(Long reportId) {
        return List.of();
    }

    @Override
    public List<DetailResult> findDetailResultsByFraudTypeId(Long fraudTypeId) {
        return List.of();
    }

    @Override
    public Long lockReportById(Long reportId) {
        return reportId;
    }

    @Override
    public List<Map<String, Object>> findSpecialTermsByReportId(Long reportId) {
        return List.of();
    }

    @Override
    public void insertSpecialTerm(Long reportId, String title, String content) {
    }

    @Override
    public void deleteSpecialTermsByReportId(Long reportId) {
    }

    @Override
    public void updateFavorite(
            Long reportId,
            boolean favorite,
            LocalDateTime favoritedAt
    ) {
    }

    @Override
    public void updateShareToken(
            Long reportId,
            String shareToken,
            LocalDateTime shareExpiresAt
    ) {
    }

    @Override
    public Map<String, Object> findShareInfoByReportId(Long reportId) {
        return null;
    }

    @Override
    public Long findReportIdByShareToken(String shareToken) {
        return null;
    }

    @Override
    public void deleteReport(Long reportId) {
    }
}