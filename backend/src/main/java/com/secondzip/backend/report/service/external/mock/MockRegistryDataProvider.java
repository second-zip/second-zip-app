package com.secondzip.backend.report.service.external.mock;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.service.external.MockApiCondition;
import com.secondzip.backend.report.service.external.client.RegistryDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Conditional(MockApiCondition.class)
public class MockRegistryDataProvider implements RegistryDataProvider {

    @Override
    public RegistryData getRegistryDataForAnalysis(
            AnalysisTarget target,
            String detailAddress,
            String buildingType
    ) {
        log.info("[MOCK] 등기부등본 조회: {}", target != null ? target.roadAddress() : null);

        RegistryData data = new RegistryData();
        data.setMortgageAmount(0L);
        data.setHasSeizure(false);
        data.setHasTrustRegistration(false);
        data.setOwnerName("홍길동");
        data.setOwnerType("INDIVIDUAL");
        data.setLandOwnerName("홍길동");
        data.setHasPostTrustInfringement(false);
        return data;
    }
}