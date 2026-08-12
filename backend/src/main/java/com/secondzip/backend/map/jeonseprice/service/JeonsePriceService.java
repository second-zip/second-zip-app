package com.secondzip.backend.map.jeonseprice.service;

import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.jeonseprice.dto.response.JeonsePriceMapResponse;

import java.time.YearMonth;

public interface JeonsePriceService {
    JeonsePriceMapResponse getJeonsePrices(
            YearMonth baseMonth,
            RegionLevel regionLevel,
            String parentRegionCode
    );
}
