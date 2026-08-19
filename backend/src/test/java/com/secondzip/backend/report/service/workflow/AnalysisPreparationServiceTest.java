package com.secondzip.backend.report.service.workflow;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.AnalysisWorkflowState;
import com.secondzip.backend.report.dto.external.BuildingData;
import com.secondzip.backend.report.dto.request.CreateReportRequest;
import com.secondzip.backend.report.dto.response.AnalysisPreparationResponse;
import com.secondzip.backend.report.enums.AnalysisRequestStatus;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.service.AddressSearchStore;
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
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
    @DisplayName("만료됐거나 없는 addressId면 분석을 시작하지 않는다")
    void rejectsExpiredAddressId() {
        AnalysisPreparationService service = new AnalysisPreparationService(
                new ExpiredAddressSearchStore(),
                new FixedBuildingClient("APARTMENT"),
                new InMemoryWorkflowStore()
        );
        ReflectionTestUtils.setField(service, "ttlSeconds", 900L);

        assertThrows(
                BusinessException.class,
                () -> service.prepare(1L, request("101동 1203호"))
        );
    }

    @Test
    @DisplayName("검색 단계의 표준화 주소를 그대로 사용한다 - 재검색하지 않는다")
    void usesStoredRoadAddressWithoutResearch() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        AnalysisPreparationService service = service("APARTMENT", store);

        AnalysisPreparationResponse response =
                service.prepare(1L, request("101동 1203호"));

        assertEquals("서울 강남구 테헤란로 152", response.getStandardizedRoadAddress());
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
        AddressSearchStore addressSearchStore = new FixedAddressSearchStore(target);
        BuildingHubClient buildingHubClient = new FixedBuildingClient(buildingType);
        AnalysisPreparationService service =
                new AnalysisPreparationService(addressSearchStore, buildingHubClient, store);
        ReflectionTestUtils.setField(service, "ttlSeconds", 900L);
        return service;
    }

    private CreateReportRequest request(String detailAddress) {
        CreateReportRequest request = new CreateReportRequest();
        request.setAddressId("test-address-id");
        request.setDetailAddress(detailAddress);
        request.setDeposit(100_000_000L);
        return request;
    }

    private static class FixedAddressSearchStore implements AddressSearchStore {
        private final AnalysisTarget target;

        private FixedAddressSearchStore(AnalysisTarget target) {
            this.target = target;
        }

        @Override
        public String save(AnalysisTarget target) {
            return "test-address-id";
        }

        @Override
        public AnalysisTarget find(String addressId) {
            return target;
        }
    }

    /** 보관 기간이 지나 후보가 사라진 상황. */
    private static class ExpiredAddressSearchStore implements AddressSearchStore {

        @Override
        public String save(AnalysisTarget target) {
            return "test-address-id";
        }

        @Override
        public AnalysisTarget find(String addressId) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "주소 정보가 만료되었습니다. 다시 검색해주세요."
            );
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
