package com.secondzip.backend.map.frauddamage.dto.response;

import com.secondzip.backend.map.frauddamage.domain.FraudDamageRegionVO;
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
            FraudDamageRegionVO fraudDamageRegionVO
    ) {
        return FraudDamageRegionResponse.builder()
                .regionCode(fraudDamageRegionVO.getRegionCode())
                .regionName(fraudDamageRegionVO.getRegionName())
                .damageHouseCount(
                        fraudDamageRegionVO.getDamageHouseCount()
                )
                .build();
    }
}

