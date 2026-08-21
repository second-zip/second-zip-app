package com.secondzip.backend.map.jeonseprice.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JeonsePriceRegion {

    private String regionCode;
    private String regionName;
    private BigDecimal priceIndex;
    private BigDecimal changeRate;
}