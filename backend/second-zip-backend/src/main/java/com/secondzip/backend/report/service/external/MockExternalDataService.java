package com.secondzip.backend.report.service.external;

import com.secondzip.backend.report.dto.BuildingData;
import com.secondzip.backend.report.dto.PriceData;
import com.secondzip.backend.report.dto.RegistryData;
import com.secondzip.backend.report.mapper.TestDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * external.api.mode=mock 일 때 사용.
 * 실제 API 대신 test_* 테이블에서 조회. 데이터 없으면 null 반환 → 판정에서 "확인 불가"(CAUTION) 처리.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class MockExternalDataService implements ExternalDataService {

    private final TestDataMapper testDataMapper;

    @Override
    public RegistryData getRegistryData(String roadAddress) {
        RegistryData data = testDataMapper.findRegistryByAddress(roadAddress);
        log.info("[MOCK] 등기 데이터 조회: {} → {}", roadAddress, data != null ? "found" : "null");
        return data;
    }

    @Override
    public BuildingData getBuildingData(String roadAddress) {
        BuildingData data = testDataMapper.findBuildingByAddress(roadAddress);
        log.info("[MOCK] 건축물 데이터 조회: {} → {}", roadAddress, data != null ? "found" : "null");
        return data;
    }

    @Override
    public PriceData getPriceData(String roadAddress) {
        PriceData data = testDataMapper.findPriceByAddress(roadAddress);
        log.info("[MOCK] 가격 데이터 조회: {} → {}", roadAddress, data != null ? "found" : "null");
        return data;
    }
}