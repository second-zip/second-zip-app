package com.secondzip.backend.map.jeonseprice.dto.response;

import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceRegionVO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class JeonsePriceRegionResponse {

    private String regionCode;
    private String regionName;
    private BigDecimal priceIndex;
    private BigDecimal changeRate;

    public static JeonsePriceRegionResponse from(
            JeonsePriceRegionVO region
    ) {
        return JeonsePriceRegionResponse.builder()
                .regionCode(region.getRegionCode())
                .regionName(region.getRegionName())
                .priceIndex(region.getPriceIndex())
                .changeRate(region.getChangeRate())
                .build();
    }
}