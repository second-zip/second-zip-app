package com.secondzip.backend.map.jeonseprice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum RebSidoRegion {

    SEOUL("서울", "11"),
    BUSAN("부산", "26"),
    DAEGU("대구", "27"),
    INCHEON("인천", "28"),
    GWANGJU("광주", "29"),
    DAEJEON("대전", "30"),
    ULSAN("울산", "31"),
    SEJONG("세종", "36"),
    GYEONGGI("경기", "41"),
    CHUNGBUK("충북", "43"),
    CHUNGNAM("충남", "44"),
    JEONNAM("전남", "46"),
    GYEONGBUK("경북", "47"),
    GYEONGNAM("경남", "48"),
    JEJU("제주", "50"),
    GANGWON("강원", "51"),
    JEONBUK("전북", "52");

    private final String rebRegionName;
    private final String regionCode;

    private static final Map<String, RebSidoRegion> REGION_MAP =
            Arrays.stream(values())
                    .collect(
                            Collectors.toMap(
                                    RebSidoRegion::getRebRegionName,
                                    region -> region
                            )
                    );

    public static Optional<RebSidoRegion> findByRebRegionName(
            String rebRegionName
    ) {
        return Optional.ofNullable(
                REGION_MAP.get(rebRegionName)
        );
    }
}
