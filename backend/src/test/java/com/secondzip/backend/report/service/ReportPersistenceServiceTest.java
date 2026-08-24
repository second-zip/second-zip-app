package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.domain.AnalysisReport;
import com.secondzip.backend.report.domain.ReportCheckResult;
import com.secondzip.backend.report.dto.CheckResultDTO;
import com.secondzip.backend.report.dto.RiskEvaluationResultDTO;
import com.secondzip.backend.report.enums.CheckType;
import com.secondzip.backend.report.enums.DataStatus;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.mapper.StubReportMapper;
import org.junit.jupiter.api.Test;

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
        RiskEvaluationResultDTO evaluation = new RiskEvaluationResultDTO(
                RiskLevel.CAUTION,
                List.of(new CheckResultDTO(
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
                        evaluation,
                        "APARTMENT",
                        false
                )
        );
        assertEquals(0, mapper.insertedCheckResults);
    }

    @Test
    void savesHousingCategoryAndTrustPropertyForChecklistGeneration() {
        CountingStubMapper mapper = new CountingStubMapper();
        ReportPersistenceService service = new ReportPersistenceService(
                mapper,
                new ObjectMapper(),
                null
        );
        RiskEvaluationResultDTO evaluation = new RiskEvaluationResultDTO(
                RiskLevel.SAFE,
                List.of(),
                List.of()
        );

        service.save(
                1L,
                "request-id",
                "서울특별시 강남구 테헤란로 1",
                "101동 1001호",
                100_000_000L,
                evaluation,
                "OFFICETEL",
                true
        );

        assertEquals("OFFICETEL", mapper.lastReport.getHousingCategory());
        assertEquals(true, mapper.lastReport.getTrustProperty());
    }

    private static class CountingStubMapper extends StubReportMapper {
        private int insertedCheckResults;
        private AnalysisReport lastReport;

        @Override
        public void insertCheckResult(ReportCheckResult checkResult) {
            insertedCheckResults++;
        }

        @Override
        public void insertReport(AnalysisReport report) {
            lastReport = report;
            report.setAnalysisReportId(1L);
        }
    }
}
