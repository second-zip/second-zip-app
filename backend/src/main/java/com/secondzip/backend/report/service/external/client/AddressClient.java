package com.secondzip.backend.report.service.external.client;

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
import java.util.List;
import java.util.Map;

// 카카오 주소 검색 API (정부 공공 API가 알아들을 수 있는 숫자 암호로 번역)
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressClient {

    private final RestTemplate restTemplate;

    // PropertyPlaceholderConfig를 통해 .env의 KAKAO_REST_API_KEY를 주입
    @Value("${KAKAO_REST_API_KEY:}")
    private String kakaoApiKey;

    // 사용자 입력 주소를 공공 API용 표준 식별값(법정동 코드 등)으로 변환
    public AnalysisTarget standardize(String inputAddress) {
        log.info("주소 표준화 요청 (카카오 API): {}", inputAddress);

        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            log.warn("카카오 API 키가 없습니다. null 반환.");
            return null;
        }

        try{
            // 1. 카카오 주소 검색 API URL 세팅
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                    .queryParam("query", inputAddress)
                    .build()
                    .encode()
                    .toUri();

            // 2. HTTP 헤더에 인증키 추가 (KakaoAK {REST_API_KEY})
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 3. API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, Map.class
            );

            // 4. JSON 응답 파싱
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("documents")) {
                throw new RuntimeException("카카오 API 응답에 documents가 없습니다.");
            }

            List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");
            if (documents.isEmpty()) {
                log.warn("검색된 주소가 없습니다: {}", inputAddress);
                return null; // 검색 실패
            }

            // 5. 첫 번째 검색 결과 가져오기
            Map<String, Object> firstDoc = documents.get(0);
            Map<String, Object> addressInfo = (Map<String, Object>) firstDoc.get("address");
            Map<String, Object> roadAddressInfo = (Map<String, Object>) firstDoc.get("road_address");

            if (addressInfo == null) {
                throw new RuntimeException("지번 주소 정보(address)가 응답에 없습니다.");
            }

            // 6. 필요한 데이터 추출
            String roadAddress = roadAddressInfo != null
                    ? (String) roadAddressInfo.get("address_name")
                    : (String) firstDoc.get("address_name");
            String bCode = (String) addressInfo.get("b_code");          // 법정동코드 (10자리)
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

            // b_code(10자리)를 5자리씩 분리 (b_code는 시군구코드 5 + 동읍면코드 5)
            String sigunguCode = bCode != null && bCode.length() >= 5 ? bCode.substring(0, 5) : "";
            String bjdongCode = bCode != null && bCode.length() >= 10 ? bCode.substring(5, 10) : "";

            log.info("주소 파싱 완료! 법정동코드: {}, 본번: {}, 부번: {}", bCode, mainNo, subNo);

            // 7. 객체 조립하여 반환
            return new AnalysisTarget(
                    inputAddress,
                    roadAddress,
                    bCode,
                    sigunguCode,
                    bjdongCode,
                    mainNo,
                    subNo,
                    roadBuildingMainNo,
                    roadBuildingSubNo,
                    "", // 건물관리번호는 비워둠 (건축HUB API 조회 시 bCode + mainNo + subNo 조합으로 우회)
                    legalDongName,
                    lotAddress
            );

        } catch (Exception e) {
            log.error("주소 표준화 중 에러 발생: {}", e.getMessage());
            return null;
        }
    }
}
