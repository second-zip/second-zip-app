package com.secondzip.backend.report.service.external.mock;

import com.secondzip.backend.report.dto.AnalysisWorkflowStateDTO;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.dto.request.ContinueAnalysisAuthRequest;
import com.secondzip.backend.report.dto.request.StartAnalysisAuthRequest;
import com.secondzip.backend.report.enums.AnalysisNextAction;
import com.secondzip.backend.report.enums.BuildingRegisterDocumentType;
import com.secondzip.backend.report.service.external.client.BuildingRegisterGatewayResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockExternalProvidersTest {

    @Test
    void registryProviderReturnsSafeDeterministicOwnerAndRightsData() {
        RegistryData data = new MockRegistryDataProvider()
                .getRegistryDataForAnalysis(null, null, null);

        assertThat(data.getMortgageAmount()).isZero();
        assertThat(data.getHasSeizure()).isFalse();
        assertThat(data.getHasTrustRegistration()).isFalse();
        assertThat(data.getOwnerName()).isEqualTo("홍길동");
        assertThat(data.getOwnerType()).isEqualTo("INDIVIDUAL");
        assertThat(data.getLandOwnerName()).isEqualTo("홍길동");
        assertThat(data.getHasPostTrustInfringement()).isFalse();
    }

    @Test
    void priceProviderReturnsFixedSaleAndDefersOfficialPriceToRegister() {
        PriceData data = new MockPriceDataProvider().getPriceData(null, "APARTMENT", null, null);

        assertThat(data.getRecentSalePrice()).isEqualTo(900_000_000L);
        assertThat(data.getOfficialPrice()).isNull();
    }

    @Test
    void buildingRegisterStartCompletesWithCodefShapedMockData() {
        StartAnalysisAuthRequest authentication = new StartAnalysisAuthRequest();
        authentication.setUserName("테스터");

        BuildingRegisterGatewayResult result = new MockBuildingRegisterGateway().start(
                new AnalysisWorkflowStateDTO(),
                BuildingRegisterDocumentType.GENERAL,
                authentication
        );

        assertCompletedRegister(result);
    }

    @Test
    void buildingRegisterContinueIsDefensivelyIdempotent() {
        BuildingRegisterGatewayResult result = new MockBuildingRegisterGateway().continueRequest(
                new AnalysisWorkflowStateDTO(),
                new ContinueAnalysisAuthRequest()
        );

        assertCompletedRegister(result);
    }

    private void assertCompletedRegister(BuildingRegisterGatewayResult result) {
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getNextAction()).isEqualTo(AnalysisNextAction.NONE);
        assertThat(result.getTwoWayState()).isNull();
        assertThat(result.getSelectionOptions()).isEmpty();
        assertThat(result.getCaptchaImage()).isNull();
        assertThat(result.getData())
                .containsEntry("resViolationStatus", "")
                .containsEntry("resUseType", "공동주택")
                .containsEntry("resBasePrice", "850000000");
    }
}
