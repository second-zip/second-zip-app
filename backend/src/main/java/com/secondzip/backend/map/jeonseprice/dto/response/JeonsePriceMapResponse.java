package com.secondzip.backend.map.jeonseprice.dto.response;

import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.jeonseprice.domain.JeonsePriceRegion;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class JeonsePriceMapResponse {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMM");

    private RegionLevel regionLevel;
    private String parentRegionCode;
    private String baseMonth;
    private List<JeonsePriceRegionResponse> regions;

    public static JeonsePriceMapResponse of(
            YearMonth baseMonth,
            RegionLevel regionLevel,
            String parentRegionCode,
            List<JeonsePriceRegion> regionVOs
    ) {
        List<JeonsePriceRegionResponse> regions =
                regionVOs.stream()
                        .map(JeonsePriceRegionResponse::from)
                        .collect(Collectors.toList());

        return JeonsePriceMapResponse.builder()
                .regionLevel(regionLevel)
                .parentRegionCode(parentRegionCode)
                .baseMonth(
                        baseMonth.format(MONTH_FORMATTER)
                )
                .regions(regions)
                .build();
    }
}