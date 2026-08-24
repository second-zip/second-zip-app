package com.secondzip.backend.report.service;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.domain.ReportShareInfo;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.ShareResponse;
import com.secondzip.backend.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReportShareService {

    private final ReportMapper reportMapper;
    private final ReportQueryService reportQueryService;

    // 공유 링크 만료 시간
    @Value("${REPORT_SHARE_TTL_DAYS:7}")
    private long shareTtlDays;

    // 공유 링크 생성
    @Transactional
    public ShareResponse createShareLink(Long accountId, Long reportId) {
        reportQueryService.validateOwnership(accountId, reportId);

        ReportShareInfo shareInfo =
                reportMapper.findShareInfoByReportId(reportId);

        String existingToken = shareInfo != null
                ? shareInfo.getShareToken()
                : null;
        LocalDateTime existingExpiresAt = shareInfo != null
                ? shareInfo.getShareExpiresAt()
                : null;

        // 공유 토큰 존재하고 유효한 경우
        if (existingToken != null
                && existingExpiresAt != null
                && existingExpiresAt.isAfter(LocalDateTime.now())) {
            return new ShareResponse(existingToken, existingExpiresAt);
        }

        String shareToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt =
                LocalDateTime.now().plusDays(Math.max(1L, shareTtlDays));

        reportMapper.updateShareToken(reportId, shareToken, expiresAt);

        log.info("리포트 공유 링크 생성. reportId={}", reportId);
        return new ShareResponse(shareToken, expiresAt);
    }

    // 공유된 리포트 열람
    public ReportDetailResponse getSharedReport(String shareToken) {
        if (shareToken == null || shareToken.isBlank()) {
            throw notFound();
        }

        Long reportId = reportMapper.findReportIdByShareToken(shareToken);
        if (reportId == null) {
            throw notFound();
        }

        Long ownerId = reportMapper.findAccountIdByReportId(reportId);
        if (ownerId == null) {
            throw notFound();
        }

        return reportQueryService.getReportDetail(ownerId, reportId);
    }

    private BusinessException notFound() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "공유된 리포트를 찾을 수 없거나 링크가 만료되었습니다."
        );
    }
}