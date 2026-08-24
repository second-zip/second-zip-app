package com.secondzip.backend.map.frauddamage.dto.response;

import com.secondzip.backend.map.frauddamage.domain.FraudDamageRegion;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class FraudDamageRegionResponse {

    private final String regionCode;
    private final String regionName;
    private final Long damageHouseCount;

    public static FraudDamageRegionResponse from(
            FraudDamageRegion fraudDamageRegion
    ) {
        return FraudDamageRegionResponse.builder()
                .regionCode(fraudDamageRegion.getRegionCode())
                .regionName(fraudDamageRegion.getRegionName())
                .damageHouseCount(
                        fraudDamageRegion.getDamageHouseCount()
                )
                .build();
    }
}

