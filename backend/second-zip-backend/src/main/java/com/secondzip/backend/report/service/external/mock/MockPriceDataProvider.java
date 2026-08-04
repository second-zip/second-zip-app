package com.secondzip.backend.report.service.external.mock;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.PriceData;
import com.secondzip.backend.report.service.external.MockApiCondition;
import com.secondzip.backend.report.service.external.client.PriceDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Conditional(MockApiCondition.class)
public class MockPriceDataProvider implements PriceDataProvider {

    @Override
    public PriceData getPriceData(AnalysisTarget target, String buildingType) {
        log.info("[MOCK] 실거래가 조회: {}, buildingType={}",
                target != null ? target.roadAddress() : null, buildingType);

        PriceData data = new PriceData();
        data.setRecentSalePrice(900_000_000L);
        data.setOfficialPrice(null); // 건축물대장 mock의 resBasePrice(850000000)로 채워짐
        return data;
    }
}