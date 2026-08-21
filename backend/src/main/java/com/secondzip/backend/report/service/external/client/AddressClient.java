package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AddressCandidateDTO;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 카카오 주소 검색 API
// (주소 검색 -> 주소 표준화 후 주소 목록(addressId) 프론트로 전달 -> 프론트에서 선택 -> 백엔드에서 해당 주소 선택)
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressClient {

    private static final String ADDRESS_SEARCH_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

    private static final String KEYWORD_SEARCH_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";

    /** 카카오 주소 검색의 결과 후보를 넉넉히 보여주기 위해 최대로 받는다. */
    private static final int SEARCH_SIZE = 30;

    // 장소 검색 폴백에서 표준화할 최대 후보 수.
    // 장소 검색 결과에는 법정동코드가 없어 후보마다 주소 검색을 한 번씩 더 호출한다.
    // 즉 이 값이 곧 추가 API 호출 수다.
    private static final int KEYWORD_FALLBACK_LIMIT = 10;

    private final RestTemplate restTemplate;

    // PropertyPlaceholderConfig를 통해 .env의 KAKAO_REST_API_KEY를 주입
    @Value("${KAKAO_REST_API_KEY:}")
    private String kakaoApiKey;

    /**
     * 검색어에 해당하는 주소 후보를 모두 반환.
     * 검색 결과가 없는 것은 오류가 아니므로 빈 목록을 반환.
     * 호출 자체가 실패한 경우에만 예외를 던짐.
     */
    public List<AddressCandidateDTO> search(String query) {
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

        List<AddressCandidateDTO> candidates = searchByAddress(query);

        // 주소 검색은 완전한 주소("판교역로 235")만 찾는다.
        // 도로명만("판교역로")이나 건물명("헬리오시티")은 0건이므로 그때만 장소 검색으로 재시도한다.
        // 평소에는 호출이 1회로 끝난다.
        if (candidates.isEmpty()) {
            candidates = searchByKeyword(query);
        }

        log.info("주소 검색 완료. query={}, 후보={}건", query, candidates.size());
        return candidates;
    }

    // 주소 검색. 응답에 법정동코드가 있어 그대로 후보가 된다.
    private List<AddressCandidateDTO> searchByAddress(String query) {
        List<AddressCandidateDTO> candidates = new ArrayList<>();
        for (Map<String, Object> document
                : requestDocuments(ADDRESS_SEARCH_URL, query, SEARCH_SIZE)) {
            AddressCandidateDTO candidate = toCandidate(document, query);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    // 장소 검색 폴백.
    // 장소 검색 응답에는 법정동코드가 없어, 각 결과의 지번주소로 주소 검색을 다시 호출해 채운다.
    // 지번주소는 건물을 유일하게 특정하므로 이 재조회는 어긋날 여지가 없다.
    private List<AddressCandidateDTO> searchByKeyword(String query) {
        List<AddressCandidateDTO> candidates = new ArrayList<>();
        Set<String> seenLotAddresses = new LinkedHashSet<>();

        for (Map<String, Object> document
                : requestDocuments(KEYWORD_SEARCH_URL, query, 15)) {
            String lotAddress = asText(document.get("address_name"));
            if (lotAddress == null || !seenLotAddresses.add(lotAddress)) {
                // 한 건물에 여러 점포가 등록돼 있으면 같은 지번주소가 반복된다.
                continue;
            }
            if (seenLotAddresses.size() > KEYWORD_FALLBACK_LIMIT) {
                break;
            }

            String placeName = asText(document.get("place_name"));
            for (Map<String, Object> resolved
                    : requestDocuments(ADDRESS_SEARCH_URL, lotAddress, 1)) {
                AddressCandidateDTO candidate = toCandidate(resolved, query);
                if (candidate != null) {
                    candidates.add(candidate.withPlaceName(placeName));
                    break;
                }
            }
        }

        log.info("장소 검색 폴백 사용. query={}, 표준화 성공={}건", query, candidates.size());
        return candidates;
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> requestDocuments(String url, String query, int size) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(url)
                    .queryParam("query", query)
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
    private AddressCandidateDTO toCandidate(Map<String, Object> document, String originalAddress) {
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
        String mountainYn = (String) addressInfo.get("mountain_yn");
        String platGbCd = "Y".equalsIgnoreCase(mountainYn)
                ? "1"
                : ("N".equalsIgnoreCase(mountainYn) ? "0" : null);
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

        AnalysisTargetDTO target = new AnalysisTargetDTO(
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
                lotAddress,
                platGbCd
        );

        return new AddressCandidateDTO(target, zoneNo);
    }
}
