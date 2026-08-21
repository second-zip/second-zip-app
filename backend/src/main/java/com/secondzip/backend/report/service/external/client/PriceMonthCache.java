package com.secondzip.backend.report.service.external.client;

import java.util.List;
import java.util.Map;

/**
 * 국토교통부 실거래가 응답의 시군구·월 단위 캐시.
 *
 * 이 API는 매물 단위 조회가 없고 시군구 전체 거래를 월 단위로 내려준다.
 * 따라서 같은 시군구의 서로 다른 매물을 분석할 때 필요한 응답이 완전히 같다.
 * 캐시가 없으면 리포트 한 건마다 (조회 개월 수 × 페이지 수)만큼 다시 호출하게
 * 되어 공공데이터포털 일일 트래픽 한도를 빠르게 소진한다.
 */
public interface PriceMonthCache {

    /**
     * 캐시된 값이 없거나 조회에 실패하면 null.
     * 그 달에 거래가 없었다는 사실도 캐시하므로, 빈 리스트와 null은 다르다.
     */
    List<Map<String, Object>> find(String cacheKey);

    void put(String cacheKey, List<Map<String, Object>> items);
}
