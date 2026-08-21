package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.AnalysisWorkflowStateDTO;
import com.secondzip.backend.report.dto.RiskEvaluationResultDTO;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.service.ReportPersistenceService;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.RiskEvaluationService;
import com.secondzip.backend.report.service.TrustPropertyResolver;
import com.secondzip.backend.report.service.SpecialTermService;
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
        FixedPriceClient priceClient = new FixedPriceClient();
        ReportDetailResponse saved = report(77L);
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                priceClient,
                riskService,
                new FixedPersistenceService(saved),
                new FixedQueryService(saved),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
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
        assertEquals(new java.math.BigDecimal("84.12"), priceClient.transactionAreaSqm);
        assertEquals(12, priceClient.transactionFloor);
    }

    @Test
    void retriesFailedFinalAnalysisWithoutRepeatingAuthentication() {
        AnalysisWorkflowStateDTO failed = state();
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
                new FixedQueryService(saved),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        ReportDetailResponse result =
                service.retry(1L, "request-id");

        assertEquals(88L, result.getAnalysisReportId());
        assertEquals(AnalysisRequestStatus.COMPLETED, store.state.getStatus());
        assertEquals(null, store.state.getFailureMessage());
    }

    @Test
    void reusesReportCreatedForSameRequestId() {
        InMemoryStore store = new InMemoryStore(state());
        ReportDetailResponse existing = report(99L);
        FixedPersistenceService persistence =
                new FixedPersistenceService(report(100L));
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                new FixedPriceClient(),
                new CapturingRiskService(),
                persistence,
                new FixedQueryService(existing, 99L),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        ReportDetailResponse result = service.execute(1L, "request-id");

        assertEquals(99L, result.getAnalysisReportId());
        assertEquals(99L, store.state.getReportId());
        assertEquals(AnalysisRequestStatus.COMPLETED, store.state.getStatus());
        assertEquals(false, persistence.called);
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
                new FixedQueryService(report(90L)),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void failsWhenViolationStatusIsUnavailable() {
        AnalysisWorkflowStateDTO state = state();
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
                new FixedQueryService(report(88L)),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void failsWhenNoTransactionOrOfficialPriceIsAvailable() {
        AnalysisWorkflowStateDTO state = state();
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resViolationStatus", "")
        );
        InMemoryStore store = new InMemoryStore(state);
        PriceDataProvider unavailablePrice =
                (target, buildingType, transactionAreaSqm, transactionFloor) -> null;
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                unavailablePrice,
                new CapturingRiskService(),
                new FixedPersistenceService(report(91L)),
                new FixedQueryService(report(91L)),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void priceLookupFailureFallsBackToVerifiedOfficialPrice() {
        // 실거래가 API의 간헐적 장애로 리포트 전체를 실패시키지 않는다.
        // 공시가격은 이미 건축물대장에서 확인했고 판정 단계에서 140% 환산해
        // 같은 척도로 쓰므로, "그 달에 거래가 없었다"와 결과 품질이 다르지 않다.
        InMemoryStore store = new InMemoryStore(state());
        CapturingRiskService riskService = new CapturingRiskService();
        ReportDetailResponse saved = report(95L);
        PriceDataProvider failedPriceLookup =
                (target, buildingType, transactionAreaSqm, transactionFloor) -> {
                    throw new IllegalStateException("RTMS unavailable");
                };
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                failedPriceLookup,
                riskService,
                new FixedPersistenceService(saved),
                new FixedQueryService(saved),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        service.execute(1L, "request-id");

        assertEquals(AnalysisRequestStatus.COMPLETED, store.state.getStatus());
        assertEquals(550_000_000L, riskService.price.getOfficialPrice());
        assertEquals(null, riskService.price.getRecentSalePrice());
    }

    @Test
    void priceLookupFailureWithoutAnyPriceStopsBeforePaidRegistry() {
        AnalysisWorkflowStateDTO state = state();
        // 공시가격도 확보하지 못한 상태
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resViolationStatus", "")
        );
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                Map.of("resViolationStatus", "")
        );
        InMemoryStore store = new InMemoryStore(state);
        CountingRegistryProvider registry = new CountingRegistryProvider();
        PriceDataProvider failedPriceLookup =
                (target, buildingType, transactionAreaSqm, transactionFloor) -> {
                    throw new IllegalStateException("RTMS unavailable");
                };
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                registry,
                failedPriceLookup,
                new CapturingRiskService(),
                new FixedPersistenceService(report(95L)),
                new FixedQueryService(report(95L)),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );

        assertTrue(exception.getMessage().contains("공시가격"));
        assertEquals(
                0,
                registry.callCount,
                "기준가가 하나도 없으면 유료 조회로 넘어가 과금하면 안 된다"
        );
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void normalNoMatchingTradeMayUseVerifiedOfficialPrice() {
        InMemoryStore store = new InMemoryStore(state());
        CapturingRiskService riskService = new CapturingRiskService();
        PriceDataProvider noMatchingTrade =
                (target, buildingType, transactionAreaSqm, transactionFloor) -> null;
        ReportDetailResponse saved = report(96L);
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                noMatchingTrade,
                riskService,
                new FixedPersistenceService(saved),
                new FixedQueryService(saved),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        ReportDetailResponse result = service.execute(1L, "request-id");

        assertEquals(96L, result.getAnalysisReportId());
        assertEquals(550_000_000L, riskService.price.getOfficialPrice());
        assertEquals(null, riskService.price.getRecentSalePrice());
    }

    @Test
    void usesStandardizedAddressForRegionJudgement() {
        AnalysisWorkflowStateDTO state = state();
        // 사용자가 건물명으로 입력한 경우. 시도명으로 시작하지 않는다.
        state.setRoadAddress("테헤란로152빌딩");
        InMemoryStore store = new InMemoryStore(state);
        CapturingRiskService riskService = new CapturingRiskService();
        ReportDetailResponse saved = report(78L);
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                new FixedRegistryClient(),
                new FixedPriceClient(),
                riskService,
                new FixedPersistenceService(saved),
                new FixedQueryService(saved),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        service.execute(1L, "request-id");

        assertEquals(
                "서울 강남구 테헤란로 152",
                riskService.roadAddress,
                "원본 입력을 그대로 넘기면 수도권이 비수도권으로 분류되어 "
                        + "HUG 보증금 한도가 7억이 아닌 5억으로 계산된다"
        );
    }

    @Test
    void skipsPaidRegistryLookupWhenViolationStatusIsUnavailable() {
        AnalysisWorkflowStateDTO state = state();
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resBasePrice", "550,000,000")
        );
        InMemoryStore store = new InMemoryStore(state);
        CountingRegistryProvider registry = new CountingRegistryProvider();
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                registry,
                new FixedPriceClient(),
                new CapturingRiskService(),
                new FixedPersistenceService(report(93L)),
                new FixedQueryService(report(93L)),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(0, registry.callCount, "유료 등기부등본 조회가 호출되면 안 된다");
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    @Test
    void skipsPaidRegistryLookupWhenPriceBasisIsUnavailable() {
        AnalysisWorkflowStateDTO state = state();
        state.getBuildingRegisterData().put(
                BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                Map.of("resViolationStatus", "")
        );
        InMemoryStore store = new InMemoryStore(state);
        CountingRegistryProvider registry = new CountingRegistryProvider();
        PriceDataProvider unavailablePrice =
                (target, buildingType, transactionAreaSqm, transactionFloor) -> null;
        AnalysisExecutionService service = new AnalysisExecutionService(
                store,
                new BuildingRegisterDataParser(),
                registry,
                unavailablePrice,
                new CapturingRiskService(),
                new FixedPersistenceService(report(94L)),
                new FixedQueryService(report(94L)),
                new StubSpecialTermService(),
                new TrustPropertyResolver()
        );

        assertThrows(
                BusinessException.class,
                () -> service.execute(1L, "request-id")
        );
        assertEquals(0, registry.callCount, "유료 등기부등본 조회가 호출되면 안 된다");
        assertEquals(AnalysisRequestStatus.FAILED, store.state.getStatus());
    }

    private AnalysisWorkflowStateDTO state() {
        AnalysisTargetDTO target = new AnalysisTargetDTO(
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
                        "resBaseDate", "20260101",
                        "resDong", "101동",
                        "resHo", "1203호",
                        "resViolationStatus", ""
                )
        );
        data.put(
                BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE,
                Map.of(
                        "resViolationStatus", "위반건축물",
                        "resDong", "101동",
                        "resHo", "1203호",
                        "resExclusiveArea", "84.12",
                        "resFloor", "12층"
                )
        );

        AnalysisWorkflowStateDTO state = new AnalysisWorkflowStateDTO();
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
                "APARTMENT",
                false,
                List.of(),
                List.of(),
                List.of()
        );
    }

    /** 유료 조회가 실제로 몇 번 일어났는지 세는 스텁. */
    private static class CountingRegistryProvider implements RegistryDataProvider {
        private int callCount;

        @Override
        public RegistryData getRegistryDataForAnalysis(
                AnalysisTargetDTO target,
                String detailAddress,
                String buildingType
        ) {
            callCount++;
            RegistryData data = new RegistryData();
            data.setMortgageAmount(0L);
            data.setHasSeizure(false);
            data.setHasTrustRegistration(false);
            return data;
        }
    }

    private static class InMemoryStore implements AnalysisWorkflowStore {
        private AnalysisWorkflowStateDTO state;

        private InMemoryStore(AnalysisWorkflowStateDTO state) {
            this.state = state;
        }

        @Override
        public void save(AnalysisWorkflowStateDTO state) {
            this.state = state;
        }

        @Override
        public AnalysisWorkflowStateDTO findOwned(
                String requestId,
                Long accountId
        ) {
            return state;
        }

        @Override
        public void delete(String requestId) {
        }

        @Override
        public String tryAcquireExecutionLock(String requestId) {
            return "test-lock-token";
        }

        @Override
        public void releaseExecutionLock(
                String requestId,
                String lockToken
        ) {
        }
    }

    private static class FixedRegistryClient extends RegistryClient {
        private FixedRegistryClient() {
            super(
                    new RestTemplate(),
                    new CodefTokenProvider(new RestTemplate()),
                    new ObjectMapper(),
                    new RegistryRequestFactory(),
                    new com.secondzip.backend.report.service.external.client.RegistryDataParser(),
                    null   // getRegistryDataForAnalysis를 오버라이드하므로 캐시를 타지 않는다
            );
        }

        @Override
        public RegistryData getRegistryDataForAnalysis(
                AnalysisTargetDTO target,
                String detailAddress,
                String buildingType
        ) {
            return new RegistryData();
        }
    }

    private static class FixedPriceClient extends PriceClient {
        private java.math.BigDecimal transactionAreaSqm;
        private Integer transactionFloor;

        private FixedPriceClient() {
            super(new RestTemplate(), null);
        }

        @Override
        public PriceData getPriceData(
                AnalysisTargetDTO target,
                String buildingType,
                java.math.BigDecimal transactionAreaSqm,
                Integer transactionFloor
        ) {
            this.transactionAreaSqm = transactionAreaSqm;
            this.transactionFloor = transactionFloor;
            PriceData price = new PriceData();
            price.setRecentSalePrice(900_000_000L);
            return price;
        }
    }

    private static class CapturingRiskService
            extends RiskEvaluationService {
        private BuildingData building;
        private PriceData price;
        private String roadAddress;

        @Override
        public RiskEvaluationResultDTO evaluate(
                RegistryData registry,
                BuildingData building,
                PriceData price,
                Long deposit,
                String roadAddress
        ) {
            this.building = building;
            this.price = price;
            this.roadAddress = roadAddress;
            return new RiskEvaluationResultDTO(
                    RiskLevel.DANGER,
                    List.of(),
                    List.of()
            );
        }
    }

    private static class FixedPersistenceService
            extends ReportPersistenceService {
        private final ReportDetailResponse response;
        private boolean called;
        private String capturedHousingCategory;
        private boolean capturedTrustProperty;

        private FixedPersistenceService(ReportDetailResponse response) {
            super(null, new ObjectMapper(), null);
            this.response = response;
        }

        @Override
        public ReportDetailResponse save(
                Long accountId,
                String requestId,
                String roadAddress,
                String detailAddress,
                Long deposit,
                RiskEvaluationResultDTO evalResult,
                String housingCategory,
                boolean trustProperty
        ) {
            called = true;
            this.capturedHousingCategory = housingCategory;
            this.capturedTrustProperty = trustProperty;
            return response;
        }
    }

    private static class FixedQueryService extends ReportQueryService {
        private final ReportDetailResponse response;
        private final Long existingReportId;

        private FixedQueryService(ReportDetailResponse response) {
            this(response, null);
        }

        private FixedQueryService(
                ReportDetailResponse response,
                Long existingReportId
        ) {
            super(null, new ObjectMapper());
            this.response = response;
            this.existingReportId = existingReportId;
        }

        @Override
        public Long findReportIdByRequestId(
                Long accountId,
                String requestId
        ) {
            return existingReportId;
        }

        @Override
        public ReportDetailResponse getReportDetail(
                Long accountId,
                Long reportId
        ) {
            return response;
        }
    }

    private static class StubSpecialTermService extends SpecialTermService {
        private StubSpecialTermService() {
            super(null, null, null, null, null);
        }

        @Override
        public List<SpecialTermView> generateAndSave(Long accountId, Long reportId) {
            return List.of();
        }
    }
}
