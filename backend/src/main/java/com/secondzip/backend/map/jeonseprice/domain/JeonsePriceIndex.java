package com.secondzip.backend.map.jeonseprice.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JeonsePriceIndex {

    private String regionCode;
    private LocalDate baseMonth;
    private BigDecimal priceIndex;
    private BigDecimal changeRate;
}