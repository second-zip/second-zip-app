package com.secondzip.backend.report.mapper;

import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.domain.ReportFraudType;
import com.secondzip.backend.report.domain.ReportShareInfo;
import com.secondzip.backend.report.domain.ReportSpecialTerm;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.VerifiedChecklistItem;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 테스트용 ReportMapper 기본 구현.
 * 모든 메서드가 아무 동작도 하지 않으며, 각 테스트는 필요한 것만 오버라이드한다.
 * ReportMapper에 메서드가 추가되면 이 파일만 수정하면 된다.
 */
public class StubReportMapper implements ReportMapper {

    @Override
    public void insertCheckResult(ReportCheckResult checkResult) {
    }

    @Override
    public void insertFraudType(ReportFraudType fraudType) {
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
    public void insertReport(AnalysisReport report) {
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
    public AnalysisReport findReportById(Long reportId) {
        return new AnalysisReport();
    }

    @Override
    public List<ReportCheckResult> findCheckResultsByReportId(Long reportId) {
        return List.of();
    }

    @Override
    public List<ReportFraudType> findFraudTypesByReportId(Long reportId) {
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
    public List<ReportSpecialTerm> findSpecialTermsByReportId(Long reportId) {
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
    public ReportShareInfo findShareInfoByReportId(Long reportId) {
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
