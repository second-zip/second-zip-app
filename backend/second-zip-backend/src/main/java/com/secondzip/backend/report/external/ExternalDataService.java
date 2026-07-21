package com.secondzip.backend.report.external;

/**
 * 분석에 필요한 외부 데이터 (등기, 건축물대장, 실거래가) 조회 계층,
 * 개발 : MockExternalDataService(목업 DB 조회) - 현재
 * 운영 : RealExternalDataService(실제 API 호출) - 예정
 */
public interface ExternalDataService {
    RegistryData getRegistryData(String roadAddress);
    BuildingData getBuildingData(String roadAddress);
    PriceData getPriceData(String roadAddress);
}
