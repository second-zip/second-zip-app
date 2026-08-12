package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.response.AnalysisPreparationResponse;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.service.external.client.AddressClient;
import com.secondzip.backend.report.service.external.client.AddressSearchCache;
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisPreparationServiceTest {

    @Test
    void apartmentRequiresTitleAndExclusiveDocuments() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        AnalysisPreparationService service = service("APARTMENT", store);

        AnalysisPreparationResponse response =
                service.prepare(1L, request("101동 1203호"));

        assertEquals(AnalysisRequestStatus.AUTH_REQUIRED, response.getStatus());
        assertEquals(
                java.util.List.of(
                        BuildingRegisterDocumentType.COLLECTIVE_TITLE,
                        BuildingRegisterDocumentType.COLLECTIVE_EXCLUSIVE
                ),
                response.getRequiredDocuments()
        );
        assertTrue(response.isAuthenticationRequired());
        assertEquals(1L, store.findOwned(response.getRequestId(), 1L).getAccountId());
    }

    @Test
    void collectiveBuildingRejectsMissingUnitAddress() {
        AnalysisPreparationService service =
                service("OFFICETEL", new InMemoryWorkflowStore());

        assertThrows(
                BusinessException.class,
                () -> service.prepare(1L, request(""))
        );
    }

    @Test
    void singleFamilyRequiresOnlyGeneralRegister() {
        AnalysisPreparationService service =
                service("SINGLE_FAMILY", new InMemoryWorkflowStore());

        AnalysisPreparationResponse response = service.prepare(1L, request(null));

        assertEquals(
                java.util.List.of(BuildingRegisterDocumentType.GENERAL),
                response.getRequiredDocuments()
        );
    }

    @Test
    @DisplayName("주소 토큰이 있으면 재검색하지 않고 사용자가 고른 주소를 그대로 쓴다")
    void usesTokenizedAddressWithoutResearching() {
        // 사용자가 검색 화면에서 고른 주소 (2번째 결과라고 가정)
        AnalysisTarget selected = target("서울 강남구 테헤란로 152", "1168010100", "11680");
        // 재검색하면 나올 엉뚱한 주소 (첫 번째 결과)
        AnalysisTarget firstHit = target("서울 서초구 테헤란로 152", "1165010100", "11650");

        FixedAddressClient addressClient = new FixedAddressClient(firstHit);
        CapturingBuildingClient buildingClient = new CapturingBuildingClient("APARTMENT");
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();

        AnalysisPreparationService service = new AnalysisPreparationService(
                addressClient, buildingClient, store, new HitCache(selected));
        ReflectionTestUtils.setField(service, "ttlSeconds", 900L);

        CreateReportRequest request = request("101동 1203호");
        request.setAddressToken("token-1234");

        AnalysisPreparationResponse response = service.prepare(1L, request);

        // 카카오 재검색이 아예 일어나지 않아야 한다
        assertEquals(0, addressClient.standardizeCallCount,
                "토큰이 있으면 카카오를 다시 부르면 안 된다");
        // 그리고 분석 대상은 사용자가 고른 그 주소여야 한다
        assertEquals("1168010100", buildingClient.lastTarget.legalDongCode());
        assertEquals(
                "서울 강남구 테헤란로 152",
                store.findOwned(response.getRequestId(), 1L).getTarget().roadAddress()
        );
    }

    @Test
    @DisplayName("토큰이 만료되면 주소 문자열로 재검색한다")
    void fallsBackToSearchWhenTokenExpired() {
        AnalysisTarget firstHit = target("서울 강남구 테헤란로 152", "1168010100", "11680");
        FixedAddressClient addressClient = new FixedAddressClient(firstHit);
        CapturingBuildingClient buildingClient = new CapturingBuildingClient("APARTMENT");

        AnalysisPreparationService service = new AnalysisPreparationService(
                addressClient, buildingClient, new InMemoryWorkflowStore(), new EmptyCache());
        ReflectionTestUtils.setField(service, "ttlSeconds", 900L);

        CreateReportRequest request = request("101동 1203호");
        request.setAddressToken("expired-token");

        service.prepare(1L, request);

        assertEquals(1, addressClient.standardizeCallCount,
                "토큰이 만료되면 폴백으로 재검색해야 한다");
    }

    @Test
    @DisplayName("토큰이 없어도 기존처럼 동작한다 - 구버전 프론트 호환")
    void worksWithoutTokenForBackwardCompatibility() {
        AnalysisTarget firstHit = target("서울 강남구 테헤란로 152", "1168010100", "11680");
        FixedAddressClient addressClient = new FixedAddressClient(firstHit);

        AnalysisPreparationService service = new AnalysisPreparationService(
                addressClient,
                new FixedBuildingClient("APARTMENT"),
                new InMemoryWorkflowStore(),
                new EmptyCache()
        );
        ReflectionTestUtils.setField(service, "ttlSeconds", 900L);

        // addressToken을 설정하지 않은 요청
        AnalysisPreparationResponse response =
                service.prepare(1L, request("101동 1203호"));

        assertEquals(AnalysisRequestStatus.AUTH_REQUIRED, response.getStatus());
        assertEquals(1, addressClient.standardizeCallCount);
    }

    private AnalysisTarget target(String roadAddress, String bCode, String sigungu) {
        return new AnalysisTarget(
                roadAddress, roadAddress, bCode, sigungu,
                bCode.substring(5), "737", "", "152", "", ""
        );
    }

    private AnalysisPreparationService service(
            String buildingType,
            AnalysisWorkflowStore store
    ) {
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
                ""
        );
        return service(buildingType, store, new FixedAddressClient(target), new EmptyCache());
    }

    private AnalysisPreparationService service(
            String buildingType,
            AnalysisWorkflowStore store,
            AddressClient addressClient,
            AddressSearchCache cache
    ) {
        BuildingHubClient buildingHubClient = new FixedBuildingClient(buildingType);
        AnalysisPreparationService service = new AnalysisPreparationService(
                addressClient, buildingHubClient, store, cache);
        ReflectionTestUtils.setField(service, "ttlSeconds", 900L);
        return service;
    }

    private CreateReportRequest request(String detailAddress) {
        CreateReportRequest request = new CreateReportRequest();
        request.setRoadAddress("서울 강남구 테헤란로 152");
        request.setDetailAddress(detailAddress);
        request.setDeposit(100_000_000L);
        return request;
    }

    private static class FixedAddressClient extends AddressClient {
        private final AnalysisTarget target;
        private int standardizeCallCount;

        private FixedAddressClient(AnalysisTarget target) {
            super(new RestTemplate());
            this.target = target;
        }

        @Override
        public AnalysisTarget standardize(String inputAddress) {
            standardizeCallCount++;
            return target;
        }
    }

    /** 토큰이 없는 상황. 항상 캐시 미스. */
    private static class EmptyCache extends AddressSearchCache {
        private EmptyCache() {
            super(null, new ObjectMapper());
        }

        @Override
        public AnalysisTarget find(String token) {
            return null;
        }
    }

    /** 토큰이 유효한 상황. 사용자가 고른 주소를 그대로 돌려준다. */
    private static class HitCache extends AddressSearchCache {
        private final AnalysisTarget target;

        private HitCache(AnalysisTarget target) {
            super(null, new ObjectMapper());
            this.target = target;
        }

        @Override
        public AnalysisTarget find(String token) {
            return token == null || token.isBlank() ? null : target;
        }
    }

    /** prepare()가 어떤 주소로 건축물대장을 조회했는지 확인하기 위한 스텁. */
    private static class CapturingBuildingClient extends BuildingHubClient {
        private final String buildingType;
        private AnalysisTarget lastTarget;

        private CapturingBuildingClient(String buildingType) {
            super(new RestTemplate());
            this.buildingType = buildingType;
        }

        @Override
        public BuildingData getBuildingData(AnalysisTarget target) {
            this.lastTarget = target;
            BuildingData data = new BuildingData();
            data.setBuildingType(buildingType);
            data.setBuildingUse("주거");
            return data;
        }
    }

    private static class FixedBuildingClient extends BuildingHubClient {
        private final String buildingType;

        private FixedBuildingClient(String buildingType) {
            super(new RestTemplate());
            this.buildingType = buildingType;
        }

        @Override
        public BuildingData getBuildingData(AnalysisTarget target) {
            BuildingData data = new BuildingData();
            data.setBuildingType(buildingType);
            data.setBuildingUse("주거");
            return data;
        }
    }

    private static class InMemoryWorkflowStore implements AnalysisWorkflowStore {
        private final Map<String, AnalysisWorkflowState> states =
                new ConcurrentHashMap<>();

        @Override
        public void save(AnalysisWorkflowState state) {
            states.put(state.getRequestId(), state);
        }

        @Override
        public AnalysisWorkflowState findOwned(String requestId, Long accountId) {
            AnalysisWorkflowState state = states.get(requestId);
            if (state == null || !state.getAccountId().equals(accountId)) {
                throw new IllegalArgumentException("not found");
            }
            return state;
        }

        @Override
        public void delete(String requestId) {
            states.remove(requestId);
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
}
