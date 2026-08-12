package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AddressCandidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code AddressClient.search()} 검증.
 *
 * <p>기존 {@code AddressClientParsingTest}가 단건 파싱을 다루므로,
 * 여기서는 목록 처리와 실패 내성에 집중한다.
 */
class AddressClientSearchTest {

    @Test
    @DisplayName("검색 결과를 모두 표준화해서 반환한다")
    void returnsEveryDocument() {
        AddressClient client = client(new StubRestTemplate(List.of(
                document("서울 강남구 테헤란로 152", "1168010100", "06236"),
                document("서울 서초구 테헤란로 152", "1165010100", "06611")
        )));

        List<AddressCandidate> results = client.search("테헤란로 152", 1, 30);

        assertEquals(2, results.size());
        assertEquals("서울 강남구 테헤란로 152", results.get(0).target().roadAddress());
        assertEquals("서울 서초구 테헤란로 152", results.get(1).target().roadAddress());
    }

    @Test
    @DisplayName("우편번호를 함께 담는다 - AnalysisTarget은 건드리지 않는다")
    void carriesZoneNoOutsideAnalysisTarget() {
        AddressClient client = client(new StubRestTemplate(List.of(
                document("서울 강남구 테헤란로 152", "1168010100", "06236")
        )));

        AddressCandidate candidate = client.search("테헤란로 152", 1, 30).get(0);

        assertEquals("06236", candidate.zoneNo());
    }

    @Test
    @DisplayName("법정동코드를 10자리에서 시군구·읍면동으로 쪼갠다")
    void splitsLegalDongCode() {
        AddressClient client = client(new StubRestTemplate(List.of(
                document("서울 강남구 테헤란로 152", "1168010100", "06236")
        )));

        AddressCandidate candidate = client.search("테헤란로 152", 1, 30).get(0);

        assertEquals("1168010100", candidate.target().legalDongCode());
        assertEquals("11680", candidate.target().sigunguCode());
        assertEquals("10100", candidate.target().bjdongCode());
    }

    @Test
    @DisplayName("지번 정보가 없는 결과는 건너뛰고 나머지는 살린다")
    void skipsDocumentsWithoutParcelInfo() {
        Map<String, Object> broken = new HashMap<>();
        broken.put("address_name", "이름만 있는 결과");
        broken.put("address", null);
        broken.put("road_address", null);

        AddressClient client = client(new StubRestTemplate(List.of(
                broken,
                document("서울 강남구 테헤란로 152", "1168010100", "06236")
        )));

        List<AddressCandidate> results = client.search("테헤란로 152", 1, 30);

        // 하나가 이상해도 전체가 실패하면 안 된다
        assertEquals(1, results.size());
        assertEquals("서울 강남구 테헤란로 152", results.get(0).target().roadAddress());
    }

    @Test
    @DisplayName("API 키가 없으면 예외 대신 빈 목록을 반환한다")
    void returnsEmptyWhenApiKeyMissing() {
        AddressClient client = new AddressClient(new StubRestTemplate(List.of()));
        ReflectionTestUtils.setField(client, "kakaoApiKey", "");

        assertTrue(client.search("테헤란로 152", 1, 30).isEmpty());
    }

    @Test
    @DisplayName("검색어가 비어 있으면 카카오를 호출하지 않는다")
    void doesNotCallKakaoForBlankQuery() {
        StubRestTemplate restTemplate = new StubRestTemplate(List.of(
                document("서울 강남구 테헤란로 152", "1168010100", "06236")
        ));
        AddressClient client = client(restTemplate);

        assertTrue(client.search("   ", 1, 30).isEmpty());
        assertEquals(0, restTemplate.callCount);
    }

    @Test
    @DisplayName("외부 호출이 실패해도 예외를 던지지 않는다")
    void swallowsUpstreamFailure() {
        AddressClient client = client(new RestTemplate() {
            @Override
            public <T> ResponseEntity<T> exchange(
                    URI url, HttpMethod method,
                    HttpEntity<?> requestEntity, Class<T> responseType
            ) {
                throw new IllegalStateException("kakao down");
            }
        });

        assertTrue(client.search("테헤란로 152", 1, 30).isEmpty());
    }

    @Test
    @DisplayName("standardize()는 첫 번째 결과만 쓰는 기존 동작을 유지한다")
    void standardizeKeepsFirstResultBehaviour() {
        AddressClient client = client(new StubRestTemplate(List.of(
                document("서울 강남구 테헤란로 152", "1168010100", "06236"),
                document("서울 서초구 테헤란로 152", "1165010100", "06611")
        )));

        assertEquals(
                "서울 강남구 테헤란로 152",
                client.standardize("테헤란로 152").roadAddress()
        );
    }

    @Test
    @DisplayName("결과가 없으면 standardize()는 null을 반환한다")
    void standardizeReturnsNullWhenNoResult() {
        AddressClient client = client(new StubRestTemplate(List.of()));

        assertNull(client.standardize("없는 주소"));
    }

    private AddressClient client(RestTemplate restTemplate) {
        AddressClient client = new AddressClient(restTemplate);
        ReflectionTestUtils.setField(client, "kakaoApiKey", "test-key");
        return client;
    }

    private Map<String, Object> document(String roadAddress, String bCode, String zoneNo) {
        Map<String, Object> address = new HashMap<>();
        address.put("b_code", bCode);
        address.put("main_address_no", "737");
        address.put("sub_address_no", "");
        address.put("address_name", "서울 강남구 역삼동 737");
        address.put("region_3depth_name", "역삼동");

        Map<String, Object> road = new HashMap<>();
        road.put("address_name", roadAddress);
        road.put("main_building_no", "152");
        road.put("sub_building_no", "");
        road.put("zone_no", zoneNo);

        Map<String, Object> document = new HashMap<>();
        document.put("address_name", "서울 강남구 역삼동 737");
        document.put("address", address);
        document.put("road_address", road);
        return document;
    }

    private static class StubRestTemplate extends RestTemplate {
        private final List<Map<String, Object>> documents;
        private int callCount;

        private StubRestTemplate(List<Map<String, Object>> documents) {
            this.documents = new ArrayList<>(documents);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(
                URI url,
                HttpMethod method,
                HttpEntity<?> requestEntity,
                Class<T> responseType
        ) {
            callCount++;
            Map<String, Object> body = new HashMap<>();
            body.put("documents", documents);
            return (ResponseEntity<T>) ResponseEntity.ok(body);
        }
    }
}
