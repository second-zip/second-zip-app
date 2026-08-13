package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressClientParsingTest {

    @Test
    void separatesParcelAndRoadBuildingNumbers() {
        RestTemplate restTemplate = new KakaoAddressRestTemplate();
        AddressClient client = new AddressClient(restTemplate);
        ReflectionTestUtils.setField(client, "kakaoApiKey", "test-key");

        AnalysisTarget target = client.search("서울특별시 강남구 테헤란로 152")
                .get(0)
                .target();

        assertEquals("서울 강남구 테헤란로 152", target.roadAddress());
        assertEquals("737", target.mainNo());
        assertEquals("", target.subNo());
        assertEquals("152", target.roadBuildingMainNo());
        assertEquals("", target.roadBuildingSubNo());
    }

    private static class KakaoAddressRestTemplate extends RestTemplate {
        @Override
        @SuppressWarnings("unchecked")
        public <T> ResponseEntity<T> exchange(
                URI url,
                HttpMethod method,
                HttpEntity<?> requestEntity,
                Class<T> responseType
        ) {
            Map<String, Object> address = Map.of(
                    "b_code", "1168010100",
                    "main_address_no", "737",
                    "sub_address_no", ""
            );
            Map<String, Object> roadAddress = Map.of(
                    "address_name", "서울 강남구 테헤란로 152",
                    "main_building_no", "152",
                    "sub_building_no", ""
            );
            Map<String, Object> document = Map.of(
                    "address_name", "서울 강남구 역삼동 737",
                    "address", address,
                    "road_address", roadAddress
            );
            return (ResponseEntity<T>) ResponseEntity.ok(
                    Map.of("documents", List.of(document))
            );
        }
    }
}
