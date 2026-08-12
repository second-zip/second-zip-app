package com.secondzip.backend.map.jeonseprice.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SigunguRegionMappingVO {

    private String regionCode;
    private String regionName;
    private String parentRegionCode;
}