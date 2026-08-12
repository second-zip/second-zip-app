package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.PriceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.secondzip.backend.report.service.external.RealApiCondition;
import org.springframework.context.annotation.Conditional;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 국토교통부 실거래가 4종 API(아파트/연립다세대/단독다가구/오피스텔) 클라이언트.
 * buildingType에 맞는 서비스 URL을 선택해 호출하고, 지번(본번/부번)이 일치하는
 * 매물을 찾아 최근 실거래가를 반환한다.
 *
 * 실패/데이터없음 시 항상 null 반환 (Mock 데이터 생성 금지 원칙 유지).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Conditional(RealApiCondition.class)
public class PriceClient implements PriceDataProvider {

    private final RestTemplate restTemplate;

    @Value("${REALTY_PRICE_API_KEY:}")
    private String apiKey;

    private static final Map<String, String> SERVICE_URLS = Map.of(
            "APARTMENT",       "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade",
            "MULTI_HOUSEHOLD", "https://apis.data.go.kr/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade",
            "MULTI_FAMILY",    "https://apis.data.go.kr/1613000/RTMSDataSvcSHTrade/getRTMSDataSvcSHTrade",
            "SINGLE_FAMILY",   "https://apis.data.go.kr/1613000/RTMSDataSvcSHTrade/getRTMSDataSvcSHTrade",
            "OFFICETEL",       "https://apis.data.go.kr/1613000/RTMSDataSvcOffiTrade/getRTMSDataSvcOffiTrade"
    );

    private static final String SUCCESS_CODE = "000";
    private static final int MAX_MONTHS_LOOKBACK = 3;
    private static final int ROWS_PER_PAGE = 100;
    private static final int MAX_PAGES_PER_MONTH = 100;

    /**
     * @param target       주소 표준화 결과 (sigunguCode, mainNo, subNo 필요)
     * @param buildingType SINGLE_FAMILY/MULTI_FAMILY/APARTMENT/MULTI_HOUSEHOLD/OFFICETEL
     */
    public PriceData getPriceData(AnalysisTarget target, String buildingType) {
        if (target == null || isBlank(target.sigunguCode())) {
            log.warn("AnalysisTarget 또는 시군구코드가 없어 실거래가 조회를 스킵합니다.");
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("실거래가 API 키가 없습니다.");
            return null;
        }

        String serviceUrl = resolveServiceUrl(buildingType);
        if (serviceUrl == null) {
            log.warn("건물유형({})에 매칭되는 실거래가 API가 없습니다.", buildingType);
            return null;
        }

        try {
            for (int monthsAgo = 0; monthsAgo < MAX_MONTHS_LOOKBACK; monthsAgo++) {
                String dealYmd = LocalDate.now().minusMonths(monthsAgo)
                        .toString().substring(0, 7).replace("-", "");

                PriceData found = fetchAndFind(serviceUrl, target, dealYmd);
                if (found != null) {
                    return found;
                }
            }
            log.warn("최근 {}개월 내 일치하는 실거래 데이터를 찾지 못했습니다: {}", MAX_MONTHS_LOOKBACK, target);
            return null;

        } catch (Exception e) {
            log.error("실거래가 조회 중 에러: {}", target, e);
            return null;
        }
    }

    private String resolveServiceUrl(String buildingType) {
        if (buildingType == null) return null;
        return SERVICE_URLS.get(buildingType);
    }

    private PriceData fetchAndFind(String serviceUrl, AnalysisTarget target, String dealYmd) {
        for (int pageNo = 1; pageNo <= MAX_PAGES_PER_MONTH; pageNo++) {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(serviceUrl)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("LAWD_CD", target.sigunguCode())
                    .queryParam("DEAL_YMD", dealYmd)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", ROWS_PER_PAGE)
                    .queryParam("_type", "json")
                    .build(true)   // 이중 인코딩 방지
                    .toUri();

            log.info("실거래가 조회 요청: dealYmd={}, pageNo={}, target={}", dealYmd, pageNo, target);

            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.warn("실거래가 응답이 비어있습니다.");
                return null;
            }

            Map<String, Object> responseMap = (Map<String, Object>) body.get("response");
            if (responseMap == null) {
                log.warn("실거래가 응답 형식이 예상과 다릅니다: {}", body);
                return null;
            }

            Map<String, Object> header = (Map<String, Object>) responseMap.get("header");
            String resultCode = header != null ? (String) header.get("resultCode") : null;
            if (!SUCCESS_CODE.equals(resultCode)) {
                log.warn("실거래가 응답 에러: code={}, msg={}",
                        resultCode, header != null ? header.get("resultMsg") : null);
                return null;
            }

            Map<String, Object> bodyMap = (Map<String, Object>) responseMap.get("body");
            if (bodyMap == null) return null;

            int totalCount = parseTotalCount(bodyMap.get("totalCount"));
            List<Map<String, Object>> items = extractItems(bodyMap);
            if (items == null || items.isEmpty()) {
                log.info("{}월 거래 내역 없음: {}", dealYmd, target);
                return null;
            }


            Map<String, Object> matched = findMatchingItem(items, target);
            if (matched != null) {
                Long amount = parseDealAmount((String) matched.get("dealAmount"));
                if (amount == null) return null;

                PriceData data = new PriceData();
                data.setRecentSalePrice(amount);
                // officialPrice(공시가격)는 이 API에 없음.
                // 건축HUB의 getBrHsprcInfo(건축물대장 주택가격 조회)로 별도 확보 필요 - 추후 처리.

                log.info("실거래가 매칭 성공: dealYmd={}, pageNo={}, amount={}원", dealYmd, pageNo, amount);
                return data;
            }

            int totalPages = Math.max(1, (totalCount + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
            if (pageNo >= totalPages) {
                return null;
            }
        }

        log.warn("{}월 실거래가 조회가 최대 페이지 수({})를 초과했습니다.", dealYmd, MAX_PAGES_PER_MONTH);
        return null;
    }

    private int parseTotalCount(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private List<Map<String, Object>> extractItems(Map<String, Object> bodyMap) {
        Object itemsObj = bodyMap.get("items");
        if (!(itemsObj instanceof Map)) return null;
        Object itemObj = ((Map<String, Object>) itemsObj).get("item");

        if (itemObj instanceof List) {
            return (List<Map<String, Object>>) itemObj;
        }
        if (itemObj instanceof Map) {
            return List.of((Map<String, Object>) itemObj);
        }
        return null;
    }

    /**
     * 응답 목록(items) 중 target의 본번/부번과 일치하는 매물을 찾는다.
     * jibun 형식: "178-84"(본번-부번) 또는 "75"(본번만, 부번 없음).
     * 단독/다가구는 jibun이 비어있는 경우가 있어 매칭 실패(null)로 처리될 수 있음.
     */
    private Map<String, Object> findMatchingItem(List<Map<String, Object>> items, AnalysisTarget target) {
        Integer targetMain = parseToInt(target.mainNo());
        Integer targetSub = parseToInt(target.subNo());

        if (targetMain == null) {
            log.warn("target의 본번을 알 수 없어 매물 매칭을 할 수 없습니다: {}", target);
            return null;
        }

        for (Map<String, Object> item : items) {
            String jibun = asText(item.get("jibun"));
            if (jibun == null || jibun.isBlank()) continue;

            int[] parsed = parseJibun(jibun);
            int itemMain = parsed[0];
            int itemSub = parsed[1];

            boolean mainMatches = itemMain == targetMain;
            boolean subMatches = (targetSub == null ? 0 : targetSub) == itemSub;

            if (mainMatches && subMatches) {
                return item;
            }
        }

        log.warn("본번/부번이 일치하는 거래를 찾지 못했습니다. target={}, 후보 {}건", target, items.size());
        return null;
    }

    /** "178-84" -> [178, 84], "75" -> [75, 0] */
    private int[] parseJibun(String jibun) {
        String[] parts = jibun.trim().split("-");
        int main = parseIntSafe(parts[0]);
        int sub = parts.length > 1 ? parseIntSafe(parts[1]) : 0;
        return new int[]{main, sub};
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    /** "0178" -> 178, null/blank -> null */
    private Integer parseToInt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** "12,000"(만원, 콤마포함) -> 120000000L(원) */
    private Long parseDealAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            long manwon = Long.parseLong(raw.replace(",", "").trim());
            return manwon * 10_000;
        } catch (NumberFormatException e) {
            log.warn("거래금액 파싱 실패: {}", raw);
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
