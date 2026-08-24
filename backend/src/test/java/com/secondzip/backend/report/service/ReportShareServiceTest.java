package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.domain.ReportShareInfo;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.ShareResponse;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.StubReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportShareServiceTest {

    @Test
    @DisplayName("공유 이력이 없으면 새 토큰과 만료 시각을 발급한다")
    void issuesNewTokenWhenNoneExists() throws Exception {
        StubMapper mapper = new StubMapper();
        ReportShareService service = service(mapper, true);

        ShareResponse response = service.createShareLink(1L, 10L);

        assertNotNull(response.getShareToken());
        assertTrue(response.getShareExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(response.getShareToken(), mapper.savedToken);
    }

    @Test
    @DisplayName("유효한 공유 링크가 있으면 재발급하지 않고 재사용한다")
    void reusesUnexpiredToken() throws Exception {
        StubMapper mapper = new StubMapper();
        mapper.shareInfo = ReportShareInfo.builder()
                .shareToken("existing-token")
                .shareExpiresAt(LocalDateTime.now().plusDays(3))
                .build();
        ReportShareService service = service(mapper, true);

        ShareResponse response = service.createShareLink(1L, 10L);

        assertEquals("existing-token", response.getShareToken());
        assertNull(mapper.savedToken);
    }

    @Test
    @DisplayName("만료된 공유 링크는 새 토큰으로 교체한다")
    void replacesExpiredToken() throws Exception {
        StubMapper mapper = new StubMapper();
        mapper.shareInfo = ReportShareInfo.builder()
                .shareToken("old-token")
                .shareExpiresAt(LocalDateTime.now().minusDays(1))
                .build();
        ReportShareService service = service(mapper, true);

        ShareResponse response = service.createShareLink(1L, 10L);

        assertNotEquals("old-token", response.getShareToken());
        assertNotNull(mapper.savedToken);
    }

    @Test
    @DisplayName("본인 리포트가 아니면 공유 링크를 만들지 않는다")
    void failsWhenNotOwner() throws Exception {
        StubMapper mapper = new StubMapper();
        ReportShareService service = service(mapper, false);

        assertThrows(
                BusinessException.class,
                () -> service.createShareLink(1L, 10L)
        );
        assertNull(mapper.savedToken);
    }

    @Test
    @DisplayName("존재하지 않거나 만료된 토큰으로 열람하면 실패한다")
    void sharedViewFailsForUnknownToken() throws Exception {
        StubMapper mapper = new StubMapper();
        mapper.reportIdByToken = null;
        ReportShareService service = service(mapper, true);

        assertThrows(
                BusinessException.class,
                () -> service.getSharedReport("unknown-token")
        );
    }

    @Test
    @DisplayName("빈 토큰으로 열람하면 조회를 시도하지 않고 실패한다")
    void sharedViewFailsForBlankToken() throws Exception {
        StubMapper mapper = new StubMapper();
        ReportShareService service = service(mapper, true);

        assertThrows(
                BusinessException.class,
                () -> service.getSharedReport("  ")
        );
        assertFalse(mapper.tokenLookupCalled);
    }

    @Test
    @DisplayName("유효한 토큰이면 로그인 없이도 리포트를 반환한다")
    void sharedViewReturnsReport() throws Exception {
        StubMapper mapper = new StubMapper();
        mapper.reportIdByToken = 10L;
        mapper.ownerId = 99L;
        ReportShareService service = service(mapper, true);

        ReportDetailResponse report = service.getSharedReport("valid-token");

        assertEquals(10L, report.getAnalysisReportId());
    }

    private ReportShareService service(StubMapper mapper, boolean owner)
            throws Exception {
        ReportShareService service = new ReportShareService(
                mapper,
                new StubQueryService(owner)
        );
        Field field = ReportShareService.class
                .getDeclaredField("shareTtlDays");
        field.setAccessible(true);
        field.set(service, 7L);
        return service;
    }

    private static class StubQueryService extends ReportQueryService {
        private final boolean owner;

        private StubQueryService(boolean owner) {
            super(null, new ObjectMapper());
            this.owner = owner;
        }

        @Override
        public void validateOwnership(Long accountId, Long reportId) {
            if (!owner) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "리포트를 찾을 수 없습니다."
                );
            }
        }

        @Override
        public ReportDetailResponse getReportDetail(
                Long accountId,
                Long reportId
        ) {
            return new ReportDetailResponse(
                    reportId,
                    "서울특별시 강남구 테헤란로 1",
                    "101동 1001호",
                    300_000_000L,
                    RiskLevel.CAUTION,
                    false,
                    "APARTMENT",
                    false,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
    }

    private static class StubMapper extends StubReportMapper {
        private ReportShareInfo shareInfo;
        private String savedToken;
        private Long reportIdByToken;
        private Long ownerId;
        private boolean tokenLookupCalled;

        @Override
        public void updateShareToken(
                Long reportId,
                String shareToken,
                LocalDateTime shareExpiresAt
        ) {
            this.savedToken = shareToken;
        }

        @Override
        public ReportShareInfo findShareInfoByReportId(Long reportId) {
            return shareInfo;
        }

        @Override
        public Long findReportIdByShareToken(String shareToken) {
            this.tokenLookupCalled = true;
            return reportIdByToken;
        }

        @Override
        public Long findAccountIdByReportId(Long reportId) {
            return ownerId;
        }
    }
}