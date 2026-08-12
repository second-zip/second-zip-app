package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.PriceData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceClientTest {

    @Test
    @DisplayName("JSON으로 요청하고 다음 페이지에서 일치 지번을 찾는다")
    void findsMatchingTradeOnNextPage() {
        PagingRestTemplate restTemplate = new PagingRestTemplate();
        PriceClient client = new PriceClient(restTemplate);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");

        AnalysisTarget target = new AnalysisTarget(
                "원본", "표준", "1168010100", "11680", "10100",
                "737", "", "152", "", ""
        );

        PriceData result = client.getPriceData(target, "APARTMENT");

        assertNotNull(result);
        assertEquals(120_000_000L, result.getRecentSalePrice());
        assertEquals(2, restTemplate.requestedUris.size());
        assertTrue(restTemplate.requestedUris.get(0).getQuery().contains("_type=json"));
        assertTrue(restTemplate.requestedUris.get(1).getQuery().contains("pageNo=2"));
    }

    private static class PagingRestTemplate extends RestTemplate {
        private final List<URI> requestedUris = new ArrayList<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) {
            requestedUris.add(url);
            boolean secondPage = url.getQuery().contains("pageNo=2");
            Map<String, Object> item = secondPage
                    ? Map.of("jibun", "737", "dealAmount", "12,000")
                    : Map.of("jibun", "1", "dealAmount", "1,000");
            Map<String, Object> body = Map.of(
                    "totalCount", 101,
                    "items", Map.of("item", List.of(item))
            );
            Map<String, Object> payload = Map.of(
                    "response", Map.of(
                            "header", Map.of("resultCode", "000", "resultMsg", "OK"),
                            "body", body
                    )
            );
            return (ResponseEntity<T>) ResponseEntity.ok(payload);
        }
    }
}
