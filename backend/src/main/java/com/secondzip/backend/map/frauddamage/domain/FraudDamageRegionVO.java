package com.secondzip.backend.map.frauddamage.domain;

import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FraudDamageRegionVO {

    private String regionCode;
    private String regionName;
    private Long damageHouseCount;
    private LocalDate baseDate;
}

