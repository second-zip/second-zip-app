package com.secondzip.backend.map.common.domain;

import com.secondzip.backend.map.common.enums.RegionLevel;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Region {

    private Long regionId;
    private String regionCode;
    private String regionName;
    private RegionLevel regionLevel;
    private Long parentRegionId;
}
