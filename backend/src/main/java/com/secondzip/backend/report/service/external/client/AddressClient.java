package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
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

// 카카오 주소 검색 API
// (주소 검색 -> 주소 표준화 후 주소 목록(addressId) 프론트로 전달 -> 프론트에서 선택 -> 백엔드에서 해당 주소 선택)
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressClient {

    private static final String SEARCH_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

    /** 카카오 주소 검색의 결과 후보를 넉넉히 보여주기 위해 최대로 받는다. */
    private static final int SEARCH_SIZE = 30;

    private final RestTemplate restTemplate;

    // PropertyPlaceholderConfig를 통해 .env의 KAKAO_REST_API_KEY를 주입
    @Value("${KAKAO_REST_API_KEY:}")
    private String kakaoApiKey;

    /**
     * 검색어에 해당하는 주소 후보를 모두 반환.
     * 검색 결과가 없는 것은 오류가 아니므로 빈 목록을 반환.
     * 호출 자체가 실패한 경우에만 예외를 던짐.
     */
    public List<AddressCandidate> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            log.warn("카카오 API 키가 없습니다.");
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "주소 검색 설정이 완료되지 않았습니다."
            );
        }

        List<Map<String, Object>> documents = requestDocuments(query);
        List<AddressCandidate> candidates = new ArrayList<>();
        for (Map<String, Object> document : documents) {
            AddressCandidate candidate = toCandidate(document, query);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        log.info("주소 검색 완료. query={}, 후보={}건", query, candidates.size());
        return candidates;
    }

    /**
     * 사용자 입력 주소를 공공 API용 표준 식별값으로 변환한다.
     *
     * 검색 결과를 서버가 보관하고 addressId로 참조하는 방식(AN_19)으로 대체된다.
     * 남아 있는 호출부가 정리되면 삭제한다.
     */
    @Deprecated
    public AnalysisTarget standardize(String inputAddress) {
        log.info("주소 표준화 요청 (카카오 API): {}", inputAddress);
        try {
            return search(inputAddress).stream()
                    .findFirst()
                    .map(AddressCandidate::target)
                    .orElse(null);
        } catch (RuntimeException e) {
            log.error("주소 표준화 중 에러 발생: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> requestDocuments(String query) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(SEARCH_URL)
                    .queryParam("query", query)
                    .queryParam("size", SEARCH_SIZE)
                    .build()
                    .encode()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            ResponseEntity<Map> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<Void>(headers), Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.get("documents") == null) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "주소 검색 응답을 해석하지 못했습니다."
                );
            }
            return (List<Map<String, Object>>) body.get("documents");

        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("카카오 주소 검색 실패. query={}, message={}", query, e.getMessage());
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "주소를 검색하지 못했습니다."
            );
        }
    }

    /** 카카오 응답 1건을 후보로 변환한다. 지번 정보가 없으면 분석에 쓸 수 없으므로 버린다. */
    @SuppressWarnings("unchecked")
    private AddressCandidate toCandidate(Map<String, Object> document, String originalAddress) {
        Map<String, Object> addressInfo = (Map<String, Object>) document.get("address");
        Map<String, Object> roadAddressInfo = (Map<String, Object>) document.get("road_address");

        // 법정동코드·본번·부번은 지번 정보에만 있다. 없으면 외부 API 조회가 불가능하다.
        if (addressInfo == null) {
            return null;
        }

        String roadAddress = roadAddressInfo != null
                ? (String) roadAddressInfo.get("address_name")
                : (String) document.get("address_name");
        String bCode = (String) addressInfo.get("b_code");
        String mainNo = (String) addressInfo.get("main_address_no");
        String subNo = (String) addressInfo.get("sub_address_no");
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
                : null;

        // b_code(10자리)를 5자리씩 분리 (시군구코드 5 + 읍면동코드 5)
        String sigunguCode = bCode != null && bCode.length() >= 5 ? bCode.substring(0, 5) : "";
        String bjdongCode = bCode != null && bCode.length() >= 10 ? bCode.substring(5, 10) : "";

        AnalysisTarget target = new AnalysisTarget(
                originalAddress,
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
