package com.secondzip.backend.map.jeonseprice.service;

import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.jeonseprice.dto.response.JeonsePriceMapResponse;

public interface JeonsePriceService {
    JeonsePriceMapResponse getJeonsePrices(
            RegionLevel regionLevel,
            String parentRegionCode
    );
}
