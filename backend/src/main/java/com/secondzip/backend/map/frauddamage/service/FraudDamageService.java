package com.secondzip.backend.map.frauddamage.service;

import com.secondzip.backend.map.common.enums.RegionLevel;
import com.secondzip.backend.map.frauddamage.dto.response.FraudDamageMapResponse;

public interface FraudDamageService {
    FraudDamageMapResponse getFraudDamages(
            RegionLevel regionLevel,
            String parentRegionCode
    );
}
