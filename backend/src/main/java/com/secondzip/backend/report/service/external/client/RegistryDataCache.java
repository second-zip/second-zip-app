package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.external.RegistryData;

/**
 * 등기부등본 조회 결과 캐시.
 *
 * <p>등기 열람은 건당 전자민원캐시가 차감되므로, 같은 문서를 짧은 시간 안에
 * 다시 조회할 때 중복 과금을 피하기 위한 장치다.
 *
 * <p>구현체는 조회 실패를 예외로 던지지 않는다. 캐시는 어디까지나 최적화이므로,
 * 캐시 저장소에 문제가 생겨도 분석 자체는 계속 진행되어야 한다.
 */
public interface RegistryDataCache {

    /**
     * @return 캐시된 값이 없거나 조회에 실패하면 {@code null}
     */
    RegistryData find(String cacheKey);

    void put(String cacheKey, RegistryData data);
}
