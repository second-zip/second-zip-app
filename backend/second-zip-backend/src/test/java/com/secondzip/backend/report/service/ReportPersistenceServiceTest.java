package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.dto.CheckResult;
import com.secondzip.backend.report.dto.DetailResult;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.response.ReportListItem;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.DetailType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.ReportMapper;
import com.secondzip.backend.report.mapper.StubReportMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportPersistenceServiceTest {

    @Test
    void failsReportCreationWhenEvidenceCannotBeSerialized() {
        CountingStubMapper mapper = new CountingStubMapper();
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
                        DataStatus.VERIFIED,
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

    private static class CountingStubMapper extends StubReportMapper {
        private int insertedCheckResults;

        @Override
        public void insertCheckResult(Map<String, Object> params) {
            insertedCheckResults++;
        }

        @Override
        public void insertReportMap(Map<String, Object> params) {
            params.put("reportId", 1L);
        }
    }
}
