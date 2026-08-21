package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.PriceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceClientTest {

    @Test
    @DisplayName("공식 XML을 읽고 모든 페이지의 동일 매물 중 계약일이 가장 최신인 거래를 고른다")
    void parsesXmlAndFindsLatestTradeAcrossPages() {
        String firstPageItems = item(
                "역삼동", "737", "excluUseAr", "84.120", "3",
                "10", "12,000", "", ""
        ).repeat(100);
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> {
            boolean secondPage = uri.getQuery().contains("pageNo=2");
            return xml(101, secondPage ? item(
                    "역삼동", "737", "excluUseAr", "84.120", "3",
                    "20", "13,000", "", ""
            ) : firstPageItems);
        });
        PriceClient client = client(restTemplate);

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNotNull(result);
        assertEquals(130_000_000L, result.getRecentSalePrice());
        assertEquals(2, restTemplate.requestedUris.size());
        assertFalse(restTemplate.requestedUris.get(0).getQuery().contains("_type=json"));
    }

    @Test
    @DisplayName("법정동·지번·전용면적·층을 모두 맞추고 취소·금액오류 후보는 건너뛴다")
    void filtersExactUnitAndSkipsCancelledOrMalformedCandidates() {
        String items = item("개포동", "737", "excluUseAr", "84.12", "3",
                "31", "99,000", "", "")
                + item("역삼동", "737", "excluUseAr", "12.00", "3",
                "30", "98,000", "", "")
                + item("역삼동", "737", "excluUseAr", "84.12", "4",
                "29", "97,000", "", "")
                + item("역삼동", "737", "excluUseAr", "84.12", "3",
                "28", "96,000", "O", "20")
                + item("역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "N/A", "", "")
                + item("역삼동", "737", "excluUseAr", "84.12", "3",
                "20", "13,000", "", "");
        PriceClient client = client(new StubRestTemplate(uri -> xml(6, items)));

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNotNull(result);
        assertEquals(130_000_000L, result.getRecentSalePrice());
    }

    @Test
    @DisplayName("더 최신인 정확 후보의 금액을 읽지 못하면 과거 거래나 이전 달로 후퇴하지 않는다")
    void rejectsUnreadableExactCandidateThatMayBeLatest() {
        String items = item("역삼동", "737", "excluUseAr", "84.12", "3",
                "27", "N/A", "", "")
                + item("역삼동", "737", "excluUseAr", "84.12", "3",
                "20", "13,000", "", "");
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(2, items));
        PriceClient client = client(restTemplate);

        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(1, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("최신 동일일자에 서로 다른 금액이 있으면 이전 달로 후퇴하지 않고 미확인 처리한다")
    void rejectsDifferentAmountsOnSameLatestDay() {
        String items = item("역삼동", "737", "excluUseAr", "84.12", "3",
                "20", "13,000", "", "")
                + item("역삼동", "737", "excluUseAr", "84.12", "3",
                "20", "14,000", "", "");
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(2, items));
        PriceClient client = client(restTemplate);

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNull(result);
        assertEquals(1, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("단독·다가구 API는 전체 연면적을 totalFloorAr로 맞추고 층은 적용하지 않는다")
    void matchesWholeBuildingAreaForSingleHouseApi() {
        PriceClient client = client(new StubRestTemplate(uri -> xml(1, item(
                "역삼동", "737", "totalFloorAr", "150.500", "",
                "15", "50,000", "", ""
        ))));

        PriceData result = client.getPriceData(
                target(), "SINGLE_FAMILY", new BigDecimal("150.5"), null
        );

        assertNotNull(result);
        assertEquals(500_000_000L, result.getRecentSalePrice());
    }

    @Test
    @DisplayName("최근 6개월 모두 정확한 거래가 없으면 null을 반환한다")
    void returnsNullAfterSixMonths() {
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(0, ""));
        PriceClient client = client(restTemplate);

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNull(result);
        assertEquals(
                6,
                restTemplate.requestedUris.size(),
                "조회 개월 수가 늘면 시군구 전체 거래를 그만큼 더 받아온다"
        );
    }

    @Test
    @DisplayName("대장과 실거래가의 면적 반올림 차이는 허용 오차 안에서 같은 매물로 본다")
    void toleratesSmallAreaRounding() {
        PriceClient client = client(new StubRestTemplate(uri -> xml(1, item(
                "역삼동", "737", "excluUseAr", "84.95", "3",
                "10", "12,000", "", ""
        ))));

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.9540"), 3
        );

        assertNotNull(result);
        assertEquals(120_000_000L, result.getRecentSalePrice());
    }

    @Test
    @DisplayName("허용 오차를 벗어난 면적은 다른 호로 보고 채택하지 않는다")
    void rejectsAreaBeyondTolerance() {
        PriceClient client = client(new StubRestTemplate(uri -> xml(1, item(
                "역삼동", "737", "excluUseAr", "84.95", "3",
                "10", "12,000", "", ""
        ))));

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("85.20"), 3
        );

        assertNull(result);
    }

    @Test
    @DisplayName("같은 시군구·같은 달은 캐시된 응답을 쓰고 API를 다시 부르지 않는다")
    void reusesCachedMonthResponse() {
        StubPriceMonthCache cache = new StubPriceMonthCache();
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(1, item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "12,000", "", ""
        )));

        PriceData first = client(restTemplate, cache).getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );
        int callsAfterFirst = restTemplate.requestedUris.size();
        PriceData second = client(restTemplate, cache).getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getRecentSalePrice(), second.getRecentSalePrice());
        assertEquals(
                callsAfterFirst,
                restTemplate.requestedUris.size(),
                "시군구 전체 거래는 매물이 달라도 같은 응답이라 다시 받을 이유가 없다"
        );
    }

    @Test
    @DisplayName("일시적인 요청 실패는 한 번 재시도한 뒤에야 실패로 확정한다")
    void retriesTransientRequestFailureOnce() {
        AtomicInteger attempts = new AtomicInteger();
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> {
            if (attempts.getAndIncrement() == 0) {
                throw new ResourceAccessException("temporary");
            }
            return xml(1, item(
                    "역삼동", "737", "excluUseAr", "84.12", "3",
                    "10", "12,000", "", ""
            ));
        });

        PriceData result = client(restTemplate).getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNotNull(result);
        assertEquals(120_000_000L, result.getRecentSalePrice());
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("월별로 역순 조회하다 정확한 거래를 찾으면 이후 과거월은 호출하지 않는다")
    void stopsAtFirstMonthWithExactTrade() {
        AtomicInteger responseIndex = new AtomicInteger();
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> {
            if (responseIndex.getAndIncrement() < 2) {
                return xml(0, "");
            }
            return xml(1, item(
                    "역삼동", "737", "excluUseAr", "84.12", "3",
                    "10", "13,000", "", ""
            ));
        });
        PriceClient client = client(restTemplate);

        PriceData result = client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        );

        assertNotNull(result);
        assertEquals(130_000_000L, result.getRecentSalePrice());
        assertEquals(3, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("공공 API 성공코드 00도 허용한다")
    void acceptsTwoDigitSuccessCode() {
        String response = xml(1, item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        )).replace("<resultCode>000</resultCode>", "<resultCode>00</resultCode>");
        PriceClient client = client(new StubRestTemplate(uri -> response));

        assertNotNull(client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
    }

    @Test
    @DisplayName("API 오류코드는 그 달 거래 없음으로 취급하지 않고 즉시 전체 조회를 중단한다")
    void stopsOnApiErrorCode() {
        String response = xml(0, "")
                .replace("<resultCode>000</resultCode>", "<resultCode>999</resultCode>");
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> response);
        PriceClient client = client(restTemplate);

        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(1, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("월 전체 페이지를 상한 내에 읽을 수 없으면 부분 최신가를 반환하지 않는다")
    void rejectsPartiallyScannedMonth() {
        String candidate = item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        );
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(10_001, candidate));
        PriceClient client = client(restTemplate);

        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(1, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("마지막 페이지까지 읽어도 누적 item 수가 totalCount보다 작으면 부분 가격을 반환하지 않는다")
    void rejectsMissingRowsEvenAtDeclaredLastPage() {
        String candidate = item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        );
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> {
            boolean secondPage = uri.getQuery().contains("pageNo=2");
            return xml(150, secondPage ? candidate : candidate.repeat(100));
        });
        PriceClient client = client(restTemplate);

        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(2, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("item 수가 totalCount를 초과하는 모순 응답은 가격을 확정하지 않는다")
    void rejectsRowsExceedingDeclaredTotalCount() {
        String candidate = item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        );
        StubRestTemplate restTemplate = new StubRestTemplate(
                uri -> xml(1, candidate + candidate)
        );

        PriceClient client = client(restTemplate);
        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(1, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("산지 대상은 응답의 숫자 지번만으로 일반 지번 거래를 채택하지 않는다")
    void mountainTargetRejectsUnqualifiedNumericJibun() {
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(1, item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        )));

        assertNull(client(restTemplate).getPriceData(
                mountainTarget(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
    }

    @Test
    @DisplayName("산지 대상은 RTMS landCd가 산지임을 확인해주면 숫자 지번도 매칭한다")
    void mountainTargetAcceptsExplicitMountainLandCode() {
        String mountainItem = item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        ).replace("<jibun>737</jibun>", "<landCd>2</landCd><jibun>737</jibun>");

        PriceData result = client(new StubRestTemplate(uri -> xml(1, mountainItem)))
                .getPriceData(
                        mountainTarget(), "APARTMENT", new BigDecimal("84.12"), 3
                );

        assertNotNull(result);
        assertEquals(130_000_000L, result.getRecentSalePrice());
    }

    @Test
    @DisplayName("부번이 있지만 숫자가 아니면 0번으로 보정해 조회하지 않는다")
    void rejectsNonNumericTargetSubNumberBeforeCallingApi() {
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(0, ""));

        assertNull(client(restTemplate).getPriceData(
                targetWithSubNo("unknown"), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(0, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("표시 지번과 RTMS 본번·부번 코드가 충돌하면 매물을 선택하지 않는다")
    void rejectsConflictingCodedLotNumbers() {
        String conflicting = item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        ).replace(
                "<jibun>737</jibun>",
                "<bonbun>0738</bonbun><bubun>0000</bubun><jibun>737</jibun>"
        );

        assertNull(client(new StubRestTemplate(uri -> xml(1, conflicting)))
                .getPriceData(
                        target(), "APARTMENT", new BigDecimal("84.12"), 3
                ));
    }

    @Test
    @DisplayName("요청한 거래월과 다른 계약년월의 응답은 오래된 가격으로 채택하지 않는다")
    void rejectsTradeWhoseDateFallsOutsideRequestedMonth() {
        String wrongMonth = item(
                "역삼동", "737", "excluUseAr", "84.12", "3",
                "10", "13,000", "", ""
        ).replace(
                "<dealDay>10</dealDay>",
                "<dealYear>1999</dealYear><dealMonth>1</dealMonth><dealDay>10</dealDay>"
        );
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> xml(1, wrongMonth));
        PriceClient client = client(restTemplate);

        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(1, restTemplate.requestedUris.size());
    }

    @Test
    @DisplayName("XML DOCTYPE과 외부 엔티티는 파싱 전에 차단한다")
    void blocksXxePayload() {
        String malicious = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE response [<!ENTITY xxe SYSTEM "file:///C:/Windows/win.ini">]>
                <response><header><resultCode>000</resultCode></header>
                <body><totalCount>1</totalCount><items><item>
                <umdNm>&xxe;</umdNm><jibun>737</jibun><excluUseAr>84.12</excluUseAr>
                <floor>3</floor><dealDay>20</dealDay><dealAmount>13,000</dealAmount>
                </item></items></body></response>
                """;
        StubRestTemplate restTemplate = new StubRestTemplate(uri -> malicious);
        PriceClient client = client(restTemplate);

        assertThrows(PriceClient.PriceLookupException.class, () -> client.getPriceData(
                target(), "APARTMENT", new BigDecimal("84.12"), 3
        ));
        assertEquals(1, restTemplate.requestedUris.size());
    }

    private PriceClient client(RestTemplate restTemplate) {
        return client(restTemplate, null);
    }

    private PriceClient client(RestTemplate restTemplate, PriceMonthCache cache) {
        PriceClient client = new PriceClient(restTemplate, cache);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        return client;
    }

    private static class StubPriceMonthCache implements PriceMonthCache {
        private final Map<String, List<Map<String, Object>>> stored = new HashMap<>();

        @Override
        public List<Map<String, Object>> find(String cacheKey) {
            return stored.get(cacheKey);
        }

        @Override
        public void put(String cacheKey, List<Map<String, Object>> items) {
            stored.put(cacheKey, items);
        }
    }

    private AnalysisTargetDTO target() {
        return new AnalysisTargetDTO(
                "원본", "서울특별시 강남구 테헤란로 11", "1168010100", "11680",
                "10100", "737", "", "11", "", "", "역삼동",
                "서울특별시 강남구 역삼동 737"
        );
    }

    private AnalysisTargetDTO mountainTarget() {
        return new AnalysisTargetDTO(
                "원본", "서울특별시 강남구 테헤란로 11", "1168010100", "11680",
                "10100", "737", "", "11", "", "", "역삼동",
                "서울특별시 강남구 역삼동 산 737", "1"
        );
    }

    private AnalysisTargetDTO targetWithSubNo(String subNo) {
        return new AnalysisTargetDTO(
                "원본", "서울특별시 강남구 테헤란로 11", "1168010100", "11680",
                "10100", "737", subNo, "11", "", "", "역삼동",
                "서울특별시 강남구 역삼동 737", "0"
        );
    }

    private static String xml(int totalCount, String items) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                  <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>
                  <body><totalCount>%d</totalCount><items>%s</items></body>
                </response>
                """.formatted(totalCount, items);
    }

    private static String item(
            String dong,
            String jibun,
            String areaKey,
            String area,
            String floor,
            String dealDay,
            String amount,
            String cancellationType,
            String cancellationDay
    ) {
        return """
                <item>
                  <umdNm>%s</umdNm><jibun>%s</jibun><%s>%s</%s><floor>%s</floor>
                  <dealDay>%s</dealDay><dealAmount>%s</dealAmount>
                  <cdealType>%s</cdealType><cdealDay>%s</cdealDay>
                </item>
                """.formatted(
                dong, jibun, areaKey, area, areaKey, floor, dealDay, amount,
                cancellationType, cancellationDay
        );
    }

    private static class StubRestTemplate extends RestTemplate {
        private final List<URI> requestedUris = new ArrayList<>();
        private final Function<URI, String> responder;

        private StubRestTemplate(Function<URI, String> responder) {
            this.responder = responder;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) {
            requestedUris.add(url);
            return (ResponseEntity<T>) ResponseEntity.ok(responder.apply(url));
        }
    }
}
