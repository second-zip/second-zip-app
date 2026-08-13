package com.secondzip.backend.report.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisSelectionOption;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.CodefTwoWayState;
import com.secondzip.backend.report.dto.RiskEvaluationResult;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.dto.response.AnalysisAuthResponse;
import com.secondzip.backend.report.dto.response.AnalysisPreparationResponse;
import com.secondzip.backend.report.dto.response.ReportDetailResponse;
import com.secondzip.backend.report.dto.response.SpecialTermView;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.enums.RiskLevel;
import com.secondzip.backend.report.enums.SimpleAuthProvider;
import com.secondzip.backend.report.service.ReportPersistenceService;
import com.secondzip.backend.report.service.ReportQueryService;
import com.secondzip.backend.report.service.RiskEvaluationService;
import com.secondzip.backend.report.service.TrustPropertyResolver;
import com.secondzip.backend.report.service.SpecialTermService;
import com.secondzip.backend.report.service.AddressSearchStore;
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
import com.secondzip.backend.report.service.external.client.BuildingRegisterDataParser;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGateway;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGatewayResult;
import com.secondzip.backend.report.service.external.client.CodefTokenProvider;
import com.secondzip.backend.report.service.external.client.PriceClient;
import com.secondzip.backend.report.service.external.client.RegistryClient;
import com.secondzip.backend.report.service.external.client.RegistryDataParser;
import com.secondzip.backend.report.service.external.client.RegistryRequestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisWorkflowE2ETest {

    @Test
    void completesCollectiveBuildingWorkflowFromPreparationToSavedReport() {
        InMemoryStore store = new InMemoryStore();
        AnalysisPreparationService preparation =
                new AnalysisPreparationService(
                        new FixedAddressSearchStore(),
                        new FixedBuildingHubClient(),
                        store
                );
        ReflectionTestUtils.setField(preparation, "ttlSeconds", 900L);
        AnalysisAuthenticationService authentication =
                new AnalysisAuthenticationService(
                        store,
                        new ScriptedBuildingRegisterGateway()
                );
        CapturingRiskService riskService = new CapturingRiskService();
        ReportDetailResponse savedReport = report(501L);
        FixedPersistenceService persistence =
                new FixedPersistenceService(savedReport);
        AnalysisExecutionService execution =
                new AnalysisExecutionService(
                        store,
                        new BuildingRegisterDataParser(),
                        new FixedRegistryClient(),
                        new FixedPriceClient(),
                        riskService,
                        persistence,
                        new FixedQueryService(savedReport),
                        new StubSpecialTermService(),
                        new TrustPropertyResolver()
                );

        AnalysisPreparationResponse prepared =
                preparation.prepare(1L, createRequest());
        String requestId = prepared.getRequestId();
        assertEquals(
                AnalysisRequestStatus.AUTH_REQUIRED,
                prepared.getStatus()
        );

        AnalysisAuthResponse response = authentication.start(
                1L,
                requestId,
                authenticationRequest()
        );
        assertStep(response, AnalysisRequestStatus.AUTH_PENDING,
                AnalysisNextAction.SIMPLE_AUTH);

        response = authentication.continueAuthentication(
                1L,
                requestId,
                continueRequest(null, null)
        );
        assertStep(response, AnalysisRequestStatus.SELECTION_REQUIRED,
                AnalysisNextAction.ADDRESS_SELECTION);

        response = authentication.continueAuthentication(
                1L,
                requestId,
                continueRequest("ADDR-1", null)
        );
        assertStep(response, AnalysisRequestStatus.SELECTION_REQUIRED,
                AnalysisNextAction.DONG_SELECTION);

        response = authentication.continueAuthentication(
                1L,
                requestId,
                continueRequest("101", null)
        );
        assertStep(response, AnalysisRequestStatus.SELECTION_REQUIRED,
                AnalysisNextAction.HO_SELECTION);

        response = authentication.continueAuthentication(
                1L,
                requestId,
                continueRequest("1304", null)
        );
        assertEquals(AnalysisRequestStatus.AUTH_REQUIRED, response.getStatus());

        response = authentication.start(
                1L,
                requestId,
                authenticationRequest()
        );
        assertStep(response, AnalysisRequestStatus.SELECTION_REQUIRED,
                AnalysisNextAction.CAPTCHA);
        assertEquals("base64-captcha-image", response.getCaptchaImage());

        response = authentication.continueAuthentication(
                1L,
                requestId,
                continueRequest(null, "4821")
        );
        assertEquals(AnalysisRequestStatus.PROCESSING, response.getStatus());

        ReportDetailResponse completed =
                execution.execute(1L, requestId);

        assertEquals(501L, completed.getAnalysisReportId());
        assertEquals(
                AnalysisRequestStatus.COMPLETED,
                store.state.getStatus()
        );
        assertEquals(501L, store.state.getReportId());
        assertTrue(riskService.building.getIsIllegalBuilding());
        assertEquals(550_000_000L, riskService.price.getOfficialPrice());
        assertEquals(2, store.state.getCompletedDocuments().size());

        // 체크리스트 생성 조건이 리포트에 함께 저장되는지 확인.
        // 건축물 유형은 준비 단계에서 확정한 값이 그대로 넘어와야 하고,
        // 등기에 신탁 흔적이 없으므로 신탁주택은 아니어야 한다.
        assertEquals("APARTMENT", persistence.housingCategory);
        assertFalse(persistence.trustProperty);
    }

    private void assertStep(
            AnalysisAuthResponse response,
            AnalysisRequestStatus status,
            AnalysisNextAction action
    ) {
        assertEquals(status, response.getStatus());
        assertEquals(action, response.getNextAction());
    }

    private CreateReportRequest createRequest() {
        CreateReportRequest request = new CreateReportRequest();
        request.setAddressId("test-address-id");
        request.setDetailAddress("101동 1304호");
        request.setDeposit(500_000_000L);
        return request;
    }

    private StartAnalysisAuthRequest authenticationRequest() {
        StartAnalysisAuthRequest request =
                new StartAnalysisAuthRequest();
        request.setUserName("홍길동");
        request.setBirthDate("19900101");
        request.setPhoneNo("01012345678");
        request.setProvider(SimpleAuthProvider.KAKAO);
        request.setConsent(true);
        return request;
    }

    private ContinueAnalysisAuthRequest continueRequest(
            String selection,
            String secureNo
    ) {
        ContinueAnalysisAuthRequest request =
                new ContinueAnalysisAuthRequest();
        request.setAuthentication(authenticationRequest());
        request.setSelectionValue(selection);
        request.setSecureNo(secureNo);
        return request;
    }

    private ReportDetailResponse report(Long id) {
        return new ReportDetailResponse(
                id,
                "서울특별시 송파구 송파대로 345",
                "101동 1304호",
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

    private static class InMemoryStore implements AnalysisWorkflowStore {
        private AnalysisWorkflowState state;
        private boolean locked;

        @Override
        public void save(AnalysisWorkflowState state) {
            this.state = state;
        }

        @Override
        public AnalysisWorkflowState findOwned(
                String requestId,
                Long accountId
        ) {
            if (state == null
                    || !state.getRequestId().equals(requestId)
                    || !state.getAccountId().equals(accountId)) {
                throw new IllegalStateException("workflow not found");
            }
            return state;
        }

        @Override
        public void delete(String requestId) {
            state = null;
        }

        @Override
        public String tryAcquireExecutionLock(String requestId) {
            if (locked) {
                return null;
            }
            locked = true;
            return "test-lock-token";
        }

        @Override
        public void releaseExecutionLock(
                String requestId,
                String lockToken
        ) {
            locked = false;
        }
    }

    private static class FixedAddressSearchStore implements AddressSearchStore {

        @Override
        public String save(AnalysisTarget target) {
            return "test-address-id";
        }

        @Override
        public AnalysisTarget find(String addressId) {
            return new AnalysisTarget(
                    "서울특별시 송파구 송파대로 345",
                    "서울 송파구 송파대로 345",
                    "1171010700",
                    "11710",
                    "10700",
                    "479",
                    "",
                    "345",
                    "",
                    "",
                    "가락동",
                    "서울 송파구 가락동 479"
            );
        }
    }

    private static class FixedBuildingHubClient
            extends BuildingHubClient {
        private FixedBuildingHubClient() {
            super(new RestTemplate());
        }

        @Override
        public BuildingData getBuildingData(AnalysisTarget target) {
            BuildingData result = new BuildingData();
            result.setBuildingType("APARTMENT");
            result.setBuildingUse("공동주택");
            return result;
        }
    }

    private static class ScriptedBuildingRegisterGateway
            implements BuildingRegisterGateway {
        private int titleContinuationStep;

        @Override
        public BuildingRegisterGatewayResult start(
                AnalysisWorkflowState state,
                BuildingRegisterDocumentType documentType,
                StartAnalysisAuthRequest request
        ) {
            if (documentType
                    == BuildingRegisterDocumentType.COLLECTIVE_TITLE) {
                return pending(
                        AnalysisNextAction.SIMPLE_AUTH,
                        List.of()
                );
            }
            return pending(AnalysisNextAction.CAPTCHA, List.of());
        }

        @Override
        public BuildingRegisterGatewayResult continueRequest(
                AnalysisWorkflowState state,
                ContinueAnalysisAuthRequest request
        ) {
            if (state.getPendingDocument()
                    == BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE) {
                assertEquals("4821", request.getSecureNo());
                return completed(Map.of(
                        "resViolationStatus", "위반건축물",
                        "resUseType", "공동주택"
                ));
            }

            titleContinuationStep++;
            return switch (titleContinuationStep) {
                case 1 -> pending(
                        AnalysisNextAction.ADDRESS_SELECTION,
                        List.of(new AnalysisSelectionOption(
                                "ADDR-1",
                                "서울 송파구 송파대로 345"
                        ))
                );
                case 2 -> {
                    assertEquals("ADDR-1", request.getSelectionValue());
                    yield pending(
                            AnalysisNextAction.DONG_SELECTION,
                            List.of(new AnalysisSelectionOption(
                                    "101",
                                    "101동"
                            ))
                    );
                }
                case 3 -> {
                    assertEquals("101", request.getSelectionValue());
                    yield pending(
                            AnalysisNextAction.HO_SELECTION,
                            List.of(new AnalysisSelectionOption(
                                    "1304",
                                    "1304호"
                            ))
                    );
                }
                case 4 -> {
                    assertEquals("1304", request.getSelectionValue());
                    yield completed(Map.of(
                            "resViolationStatus", "",
                            "resBasePrice", "550,000,000",
                            "resUseType", "공동주택"
                    ));
                }
                default -> throw new IllegalStateException(
                        "unexpected continuation"
                );
            };
        }

        private BuildingRegisterGatewayResult pending(
                AnalysisNextAction action,
                List<AnalysisSelectionOption> options
        ) {
            return new BuildingRegisterGatewayResult(
                    false,
                    action,
                    new CodefTwoWayState(
                            1,
                            2,
                            "test-jti",
                            "123456789"
                    ),
                    options,
                    action == AnalysisNextAction.CAPTCHA
                            ? "base64-captcha-image"
                            : null,
                    null
            );
        }

        private BuildingRegisterGatewayResult completed(
                Map<String, Object> data
        ) {
            return new BuildingRegisterGatewayResult(
                    true,
                    AnalysisNextAction.NONE,
                    null,
                    List.of(),
                    null,
                    data
            );
        }
    }

    private static class FixedRegistryClient extends RegistryClient {
        private FixedRegistryClient() {
            super(
                    new RestTemplate(),
                    new CodefTokenProvider(new RestTemplate()),
                    new ObjectMapper(),
                    new RegistryRequestFactory(),
                    new RegistryDataParser(),
                    null   // getRegistryDataForAnalysis를 오버라이드하므로 캐시를 타지 않는다
            );
        }

        @Override
        public RegistryData getRegistryDataForAnalysis(
                AnalysisTarget target,
                String detailAddress,
                String buildingType
        ) {
            RegistryData result = new RegistryData();
            result.setMortgageAmount(100_000_000L);
            result.setHasSeizure(false);
            result.setHasTrustRegistration(false);
            result.setHasPostTrustInfringement(false);
            result.setOwnerName("홍길동");
            result.setOwnerType("INDIVIDUAL");
            return result;
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
            PriceData result = new PriceData();
            result.setRecentSalePrice(900_000_000L);
            return result;
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
        private String housingCategory;
        private boolean trustProperty;

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
                RiskEvaluationResult evalResult,
                String housingCategory,
                boolean trustProperty
        ) {
            this.housingCategory = housingCategory;
            this.trustProperty = trustProperty;
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
        public Long findReportIdByRequestId(
                Long accountId,
                String requestId
        ) {
            return null;
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
