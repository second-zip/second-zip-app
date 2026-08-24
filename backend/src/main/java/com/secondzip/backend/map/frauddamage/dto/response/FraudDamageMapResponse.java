package com.secondzip.backend.map.frauddamage.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.frauddamage.domain.FraudDamageRegion;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class FraudDamageMapResponse {
    private final RegionLevel regionLevel;

    //시군구 조회일 때, 선택된 시도 정보
    //시도 조회일 때는 null
    private final String parentRegionCode;
    private final String parentRegionName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate baseDate;

    /*
    * 현재 조회 범위 내 피해 주택 수의 합계
    * 시도 조회 -> 전국 피해주택 수 합계
    * 시군구 조회 -> 선택한 시도의 피해주택 수 합계
    */
    private final Long scopeTotalDamageHouseCount;

    private final List<FraudDamageRegionResponse> regions;

    public static FraudDamageMapResponse of(
            RegionLevel regionLevel,
            String parentRegionCode,
            String parentRegionName,
            LocalDate baseDate,
            Long scopeTotalDamageHouseCount,
            List<FraudDamageRegion> fraudDamageRegions
    ) {
        List<FraudDamageRegionResponse> regions =
                fraudDamageRegions.stream()
                        .map(FraudDamageRegionResponse::from)
                        .collect(Collectors.toList());

        return FraudDamageMapResponse.builder()
                .regionLevel(regionLevel)
                .parentRegionCode(parentRegionCode)
                .parentRegionName(parentRegionName)
                .baseDate(baseDate)
                .scopeTotalDamageHouseCount(
                        scopeTotalDamageHouseCount
                )
                .regions(regions)
                .build();
    }


}
