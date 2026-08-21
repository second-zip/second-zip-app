package com.secondzip.backend.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.mapper.StubReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReportFavoriteServiceTest {

    @Test
    @DisplayName("즐겨찾기 추가 시 favorite을 true로, 시각을 함께 저장한다")
    void addFavoriteSetsTrueWithTimestamp() {
        StubMapper mapper = new StubMapper();
        ReportPersistenceService service = service(mapper, true);

        service.addFavorite(1L, 10L);

        assertEquals(10L, mapper.reportId);
        assertEquals(true, mapper.favorite);
        assertNotNull(mapper.favoritedAt);
    }

    @Test
    @DisplayName("즐겨찾기 해제 시 favorite을 false로, 시각을 null로 되돌린다")
    void removeFavoriteClearsTimestamp() {
        StubMapper mapper = new StubMapper();
        ReportPersistenceService service = service(mapper, true);

        service.removeFavorite(1L, 10L);

        assertEquals(false, mapper.favorite);
        assertNull(mapper.favoritedAt);
    }

    @Test
    @DisplayName("본인 리포트가 아니면 추가하지 않고 실패한다")
    void addFavoriteFailsWhenNotOwner() {
        StubMapper mapper = new StubMapper();
        ReportPersistenceService service = service(mapper, false);

        assertThrows(
                BusinessException.class,
                () -> service.addFavorite(1L, 10L)
        );
        assertFalse(mapper.called);
    }

    @Test
    @DisplayName("본인 리포트가 아니면 해제하지 않고 실패한다")
    void removeFavoriteFailsWhenNotOwner() {
        StubMapper mapper = new StubMapper();
        ReportPersistenceService service = service(mapper, false);

        assertThrows(
                BusinessException.class,
                () -> service.removeFavorite(1L, 10L)
        );
        assertFalse(mapper.called);
    }

    private ReportPersistenceService service(StubMapper mapper, boolean owner) {
        return new ReportPersistenceService(
                mapper,
                new ObjectMapper(),
                new StubQueryService(owner)
        );
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
    }

    private static class StubMapper extends StubReportMapper {
        private boolean called;
        private Long reportId;
        private boolean favorite;
        private LocalDateTime favoritedAt;

        @Override
        public void updateFavorite(
                Long reportId,
                boolean favorite,
                LocalDateTime favoritedAt
        ) {
            this.called = true;
            this.reportId = reportId;
            this.favorite = favorite;
            this.favoritedAt = favoritedAt;
        }
    }
}