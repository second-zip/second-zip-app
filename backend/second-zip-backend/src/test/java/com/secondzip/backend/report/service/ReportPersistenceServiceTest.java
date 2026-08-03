package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.ReportMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportPersistenceServiceTest {

    @Test
    void failsReportCreationWhenEvidenceCannotBeSerialized() {
        StubReportMapper mapper = new StubReportMapper();
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value)
                    throws JsonProcessingException {
                throw new JsonProcessingException("serialization failed") { };
            }
        };
        ReportPersistenceService service = new ReportPersistenceService(
                mapper,
                failingObjectMapper,
                null
        );
        RiskEvaluationResult evaluation = new RiskEvaluationResult(
                RiskLevel.CAUTION,
                List.of(new CheckResult(
                        CheckType.MORTGAGE_EXISTENCE,
                        RiskLevel.CAUTION,
                        Map.of("mortgageAmount", 100L)
                )),
                List.of()
        );

        assertThrows(
                BusinessException.class,
                () -> service.save(
                        1L,
                        "request-id",
                        "서울특별시 강남구 테헤란로 1",
                        null,
                        100_000_000L,
                        evaluation
                )
        );
        assertEquals(0, mapper.insertedCheckResults);
    }

    private static class StubReportMapper implements ReportMapper {
        private int insertedCheckResults;

        @Override
        public void insertCheckResult(Map<String, Object> params) {
            insertedCheckResults++;
        }

        @Override
        public void insertFraudTypeMap(Map<String, Object> params) {
        }

        @Override
        public void insertDetailResult(
                Long fraudTypeId,
                DetailType detailType,
                RiskLevel riskLevel
        ) {
        }

        @Override
        public void insertReportMap(Map<String, Object> params) {
            params.put("reportId", 1L);
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
        public void deleteReport(Long reportId) {
        }

        @Override
        public void updateFavorite(
                Long reportId,
                boolean favorite,
                LocalDateTime favoritedAt
        ) {
        }
    }
}
