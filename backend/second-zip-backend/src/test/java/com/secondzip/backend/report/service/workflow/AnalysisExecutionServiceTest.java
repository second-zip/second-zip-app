package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.service.ReportPersistenceService;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.RiskEvaluationService;
import com.secondzip.backend.report.service.external.client.BuildingRegisterDataParser;
import com.secondzip.backend.report.service.external.client.CodefTokenProvider;
import com.secondzip.backend.report.service.external.client.PriceClient;
import com.secondzip.backend.report.service.external.client.PriceDataProvider;
import com.secondzip.backend.report.service.external.client.RegistryClient;
import com.secondzip.backend.report.service.external.client.RegistryDataProvider;
import com.secondzip.backend.report.service.external.client.RegistryRequestFactory;
import com.secondzip.backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisExecutionServiceTest {

    @Test
    void combinesAuthenticatedBuildingRegisterWithRegistryAndPrice() {
        InMemoryStore store = new InMemoryStore(state());
        CapturingRiskService riskService = new CapturingRiskService();
        ReportDetailResponse saved = report(77L);
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                new FixedPriceClient(),
                riskService,
                new FixedPersistenceService(saved),
                new FixedQueryService(saved)
        );

        ReportDetailResponse result =
                service.execute(1L, "request-id");

        assertEquals(77L, result.getAnalysisReportId());
        assertEquals(
                AnalysisRequestStatus.COMPLETED,
                store.state.getStatus()
        );
        assertEquals(77L, store.state.getReportId());
        assertTrue(store.state.getBuildingRegisterData().isEmpty());
        assertTrue(riskService.building.getIsIllegalBuilding());
        assertEquals(550_000_000L, riskService.price.getOfficialPrice());
    }

    @Test
    void retriesFailedFinalAnalysisWithoutRepeatingAuthentication() {
        AnalysisWorkflowState failed = state();
        failed.setStatus(AnalysisRequestStatus.FAILED);
        failed.setCompletedDocuments(failed.getRequiredDocuments());
        failed.setFailureMessage("temporary failure");
        InMemoryStore store = new InMemoryStore(failed);
        ReportDetailResponse saved = report(88L);
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                new FixedPriceClient(),
                new CapturingRiskService(),
                new FixedPersistenceService(saved),
                new FixedQueryService(saved)
        );

        ReportDetailResponse result =
                service.retry(1L, "request-id");

        assertEquals(88L, result.getAnalysisReportId());
        assertEquals(AnalysisRequestStatus.COMPLETED, store.state.getStatus());
        assertEquals(null, store.state.getFailureMessage());
    }

    @Test
    void failsWhenRegistryDataIsUnavailable() {
        InMemoryStore store = new InMemoryStore(state());
        RegistryDataProvider unavailableRegistry =
                (target, detailAddress, buildingType) -> null;
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                unavailableRegistry,
                new FixedPriceClient(),
                new CapturingRiskService(),
                new FixedPersistenceService(report(90L)),
                new FixedQueryService(report(90L))
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void failsWhenViolationStatusIsUnavailable() {
        AnalysisWorkflowState state = state();
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resBasePrice", "550,000,000")
        );
        InMemoryStore store = new InMemoryStore(state);
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                new FixedPriceClient(),
                new CapturingRiskService(),
                new FixedPersistenceService(report(92L)),
                new FixedQueryService(report(92L))
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void failsWhenNoTransactionOrOfficialPriceIsAvailable() {
        AnalysisWorkflowState state = state();
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resViolationStatus", "")
        );
        InMemoryStore store = new InMemoryStore(state);
        PriceDataProvider unavailablePrice =
                (target, buildingType) -> null;
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                unavailablePrice,
                new CapturingRiskService(),
                new FixedPersistenceService(report(91L)),
                new FixedQueryService(report(91L))
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    private AnalysisWorkflowState state() {
        AnalysisTarget target = new AnalysisTarget(
                "서울 강남구 테헤란로 152",
                "서울 강남구 테헤란로 152",
                "1168010100",
                "11680",
                "10100",
                "737",
                "",
                "152",
                "",
                "",
                "대치동",
                "서울 강남구 대치동 737"
        );
        Map<BuildingRegisterDocumentType, Map<String, Object>> data =
                new LinkedHashMap<>();
        data.put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of(
                        "resBasePrice", "550,000,000",
                        "resViolationStatus", ""
                )
        );
        data.put(
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                Map.of("resViolationStatus", "위반건축물")
        );

        AnalysisWorkflowState state = new AnalysisWorkflowState();
        state.setRequestId("request-id");
        state.setAccountId(1L);
        state.setRoadAddress(target.roadAddress());
        state.setDetailAddress("101동 1203호");
        state.setDeposit(500_000_000L);
        state.setTarget(target);
        state.setBuildingType("APARTMENT");
        state.setBuildingUse("공동주택");
        state.setRequiredDocuments(List.of(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
        ));
        state.setStatus(AnalysisRequestStatus.PROCESSING);
        state.setBuildingRegisterData(data);
        return state;
    }

    private ReportDetailResponse report(Long id) {
        return new ReportDetailResponse(
                id,
                "서울 강남구 테헤란로 152",
                "101동 1203호",
                500_000_000L,
                RiskLevel.DANGER,
                false,
                List.of(),
                List.of()
        );
    }

    private static class InMemoryStore implements AnalysisWorkflowStore {
        private AnalysisWorkflowState state;

        private InMemoryStore(AnalysisWorkflowState state) {
            this.state = state;
        }

        @Override
        public void save(AnalysisWorkflowState state) {
            this.state = state;
        }

        @Override
        public AnalysisWorkflowState findOwned(
                String requestId,
                Long accountId
        ) {
            return state;
        }

        @Override
        public void delete(String requestId) {
        }
    }

    private static class FixedRegistryClient extends RegistryClient {
        private FixedRegistryClient() {
            super(
                    new RestTemplate(),
                    new CodefTokenProvider(new RestTemplate()),
                    new ObjectMapper(),
                    new RegistryRequestFactory(),
                    new com.secondzip.backend.report.service.external.client.RegistryDataParser()
            );
        }

        @Override
        public RegistryData getRegistryDataForAnalysis(
                AnalysisTarget target,
                String detailAddress,
                String buildingType
        ) {
            return new RegistryData();
        }
    }

    private static class FixedPriceClient extends PriceClient {
        private FixedPriceClient() {
            super(new RestTemplate());
        }

        @Override
        public PriceData getPriceData(
                AnalysisTarget target,
                String buildingType
        ) {
            PriceData price = new PriceData();
            price.setRecentSalePrice(900_000_000L);
            return price;
        }
    }

    private static class CapturingRiskService
            extends RiskEvaluationService {
        private BuildingData building;
        private PriceData price;

        @Override
        public RiskEvaluationResult evaluate(
                RegistryData registry,
                BuildingData building,
                PriceData price,
                Long deposit,
                String roadAddress
        ) {
            this.building = building;
            this.price = price;
            return new RiskEvaluationResult(
                    RiskLevel.DANGER,
                    List.of(),
                    List.of()
            );
        }
    }

    private static class FixedPersistenceService
            extends ReportPersistenceService {
        private final ReportDetailResponse response;

        private FixedPersistenceService(ReportDetailResponse response) {
            super(null, new ObjectMapper(), null);
            this.response = response;
        }

        @Override
        public ReportDetailResponse save(
                Long accountId,
                String roadAddress,
                String detailAddress,
                Long deposit,
                RiskEvaluationResult evalResult
        ) {
            return response;
        }
    }

    private static class FixedQueryService extends ReportQueryService {
        private final ReportDetailResponse response;

        private FixedQueryService(ReportDetailResponse response) {
            super(null, new ObjectMapper());
            this.response = response;
        }

        @Override
        public ReportDetailResponse getReportDetail(
                Long accountId,
                Long reportId
        ) {
            return response;
        }
    }
}
