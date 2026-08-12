package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AddressCandidate;
import com.secondzip.backend.report.dto.AnalysisTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 카카오 주소 검색 API (정부 공공 API가 알아들을 수 있는 숫자 암호로 번역)
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressClient {

    private static final String SEARCH_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

    private final RestTemplate restTemplate;

    // PropertyPlaceholderConfig를 통해 .env의 KAKAO_REST_API_KEY를 주입
    @Value("${KAKAO_REST_API_KEY:}")
    private String kakaoApiKey;

    /**
     * 검색 결과 전체를 표준화해서 반환한다.
     *
     * 결과 하나의 파싱이 실패해도 나머지는 살린다. 화면에 후보를 보여주는 용도이므로
     * 일부가 빠지는 것이 전부 실패하는 것보다 낫다.
     *
     * 실패 시 빈 목록. null을 반환하지 않는다.
     */
    public List<AddressCandidate> search(String inputAddress, int page, int size) {
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            log.warn("카카오 API 키가 없습니다.");
            return List.of();
        }
        if (inputAddress == null || inputAddress.isBlank()) {
            return List.of();
        }

        log.info("주소 검색 요청 (카카오 API): {}", inputAddress);

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(SEARCH_URL)
                    .queryParam("query", inputAddress)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            ResponseEntity<Map> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(headers), Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("documents")) {
                log.warn("카카오 API 응답에 documents가 없습니다.");
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents =
                    (List<Map<String, Object>>) body.get("documents");

            List<AddressCandidate> candidates = new ArrayList<>();
            for (Map<String, Object> doc : documents) {
                AddressCandidate candidate = toCandidate(inputAddress, doc);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
            return candidates;

        } catch (Exception e) {
            log.error("주소 검색 중 에러 발생: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 사용자 입력 주소를 공공 API용 표준 식별값으로 변환한다.
     *
     * 검색 결과가 여러 개여도 첫 번째만 쓴다. 사용자가 실제로 고른 것과
     * 다를 수 있으므로, 새 흐름에서는 addressToken을 쓰고 이 메서드는
     * 토큰이 없을 때의 폴백으로만 남는다.
     */
    public AnalysisTarget standardize(String inputAddress) {
        return search(inputAddress, 1, 1).stream()
                .findFirst()
                .map(AddressCandidate::target)
                .orElse(null);
    }

    /** 카카오 document 한 건을 표준화한다. 지번 정보가 없으면 null. */
    private AddressCandidate toCandidate(String inputAddress, Map<String, Object> doc) {
        if (doc == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> addressInfo = (Map<String, Object>) doc.get("address");
        @SuppressWarnings("unchecked")
        Map<String, Object> roadAddressInfo = (Map<String, Object>) doc.get("road_address");

        // 지번 정보가 없으면 법정동코드를 못 뽑아 공공 API를 부를 수 없다
        if (addressInfo == null) {
            log.warn("지번 주소 정보(address)가 없는 결과를 건너뜁니다.");
            return null;
        }

        String roadAddress = roadAddressInfo != null
                ? (String) roadAddressInfo.get("address_name")
                : (String) doc.get("address_name");
        String bCode = (String) addressInfo.get("b_code");           // 법정동코드 (10자리)
        String mainNo = (String) addressInfo.get("main_address_no"); // 본번
        String subNo = (String) addressInfo.get("sub_address_no");   // 부번 (없으면 "")
        String legalDongName = (String) addressInfo.get("region_3depth_name");
        String lotAddress = (String) addressInfo.get("address_name");
        String roadBuildingMainNo = roadAddressInfo != null
                ? (String) roadAddressInfo.get("main_building_no")
                : "";
        String roadBuildingSubNo = roadAddressInfo != null
                ? (String) roadAddressInfo.get("sub_building_no")
                : "";
        String zoneNo = roadAddressInfo != null
                ? (String) roadAddressInfo.get("zone_no")
                : "";

        // b_code(10자리)를 5자리씩 분리 (시군구코드 5 + 동읍면코드 5)
        String sigunguCode = bCode != null && bCode.length() >= 5 ? bCode.substring(0, 5) : "";
        String bjdongCode = bCode != null && bCode.length() >= 10 ? bCode.substring(5, 10) : "";

        AnalysisTarget target = new AnalysisTarget(
                inputAddress,
                roadAddress,
                bCode,
                sigunguCode,
                bjdongCode,
                mainNo,
                subNo,
                roadBuildingMainNo,
                roadBuildingSubNo,
                "", // 건물관리번호는 비워둠 (건축HUB는 bCode + mainNo + subNo 조합으로 우회)
                legalDongName,
                lotAddress
        );

        return new AddressCandidate(target, zoneNo);
    }
}