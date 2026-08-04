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
import com.secondzip.backend.report.service.external.client.BuildingHubClient;
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
        AddressClient addressClient = new FixedAddressClient(target);
        BuildingHubClient buildingHubClient = new FixedBuildingClient(buildingType);
        AnalysisPreparationService service =
                new AnalysisPreparationService(addressClient, buildingHubClient, store);
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

        private FixedAddressClient(AnalysisTarget target) {
            super(new RestTemplate());
            this.target = target;
        }

        @Override
        public AnalysisTarget standardize(String inputAddress) {
            return target;
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
