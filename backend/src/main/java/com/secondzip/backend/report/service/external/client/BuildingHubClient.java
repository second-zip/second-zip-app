package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.BuildingData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 건축HUB 건축물대장정보 서비스 (BldRgstHubService) - 표제부 조회(getBrTitleInfo) 클라이언트.
 * AnalysisTarget(주소 표준화 결과)을 받아 건축물 용도, 위반건축물 여부 등을 조회한다.
 *
 * 주의: 위반건축물 여부(violBldgYn)는 공식 기술문서(요청/응답 명세)에는 명시되어 있지 않음.
 *      실제 응답에 포함될 수 있어 방어적으로 파싱하되, 없으면 null(확인불가)로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuildingHubClient {

    private final RestTemplate restTemplate;

    @Value("${BUILDING_HUB_API_KEY:}")
    private String apiKey;

    private static final String BASE_URL =
            "https://apis.data.go.kr/1613000/BldRgstHubService/getBrTitleInfo";

    public BuildingData getBuildingData(AnalysisTarget target) {
        if (target == null) {
            log.warn("AnalysisTarget이 null이라 건축HUB 조회를 스킵합니다.");
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("건축HUB API 키가 없습니다.");
            return null;
        }
        if (isBlank(target.sigunguCode()) || isBlank(target.bjdongCode())) {
            log.warn("시군구코드 또는 법정동코드가 없어 건축HUB 조회를 스킵합니다: {}", target);
            return null;
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL)
                    .queryParam("serviceKey", apiKey)
                    .queryParam("sigunguCd", target.sigunguCode())
                    .queryParam("bjdongCd", target.bjdongCode())
                    .queryParam("bun", normalizeLotNumber(target.mainNo()))
                    .queryParam("ji", normalizeLotNumber(target.subNo()))
                    .queryParam("numOfRows", 100)
                    .queryParam("pageNo", 1)
                    .queryParam("_type", "json")
                    .build(true)   // 이중 인코딩 방지
                    .toUri();

            log.info("건축HUB 조회 요청");

            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.warn("건축HUB 응답이 비어있습니다.");
                return null;
            }

            Map<String, Object> responseMap = (Map<String, Object>) body.get("response");
            if (responseMap == null) {
                log.warn("건축HUB 응답 형식이 예상과 다릅니다.");
                return null;
            }

            Map<String, Object> header = (Map<String, Object>) responseMap.get("header");
            String resultCode = header != null ? (String) header.get("resultCode") : null;
            if (!"00".equals(resultCode)) {
                log.warn("건축HUB 응답 에러: code={}, msg={}",
                        resultCode, header != null ? header.get("resultMsg") : null);
                return null;
            }

            Map<String, Object> bodyMap = (Map<String, Object>) responseMap.get("body");
            if (bodyMap == null) {
                log.warn("건축HUB 응답에 body가 없습니다.");
                return null;
            }

            Map<String, Object> item = extractFirstItem(bodyMap);
            if (item == null) {
                log.warn("건축물대장 표제부 조회 결과가 없습니다.");
                return null;
            }


            String mainPurpsCdNm = (String) item.get("mainPurpsCdNm"); // 주용도코드명
            String etcPurps = (String) item.get("etcPurps");           // 기타용도
            String buildingName = (String) item.get("bldNm");          // 건물명
            String violBldgYn = (String) item.get("violBldgYn");       // 공식 문서엔 없어서 방어적으로 확인

            BuildingData data = new BuildingData();
            data.setBuildingUse(mainPurpsCdNm);
            data.setBuildingType(inferBuildingType(mainPurpsCdNm, etcPurps, buildingName));
            data.setIsIllegalBuilding(parseNullableYn(violBldgYn));

            log.info("건축HUB 조회 완료: buildingUse={}, buildingType={}, isIllegalBuilding={}",
                    data.getBuildingUse(), data.getBuildingType(), data.getIsIllegalBuilding());

            return data;

        } catch (Exception e) {
            log.error("건축HUB 조회 중 에러: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * items.item은 결과가 1건이면 Map, 여러 건이면 List로 오는 경우가 있어 방어적으로 처리.
     */
    private Map<String, Object> extractFirstItem(Map<String, Object> bodyMap) {
        Object itemsObj = bodyMap.get("items");
        if (!(itemsObj instanceof Map)) {
            return null;
        }
        Object itemObj = ((Map<String, Object>) itemsObj).get("item");

        if (itemObj instanceof List) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) itemObj;
            return items.stream()
                    .max((left, right) -> Integer.compare(residentialScore(left), residentialScore(right)))
                    .orElse(null);
        }
        if (itemObj instanceof Map) {
            return (Map<String, Object>) itemObj;
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    static String normalizeLotNumber(String value) {
        if (value == null || value.isBlank()) {
            return "0000";
        }
        try {
            return String.format(Locale.ROOT, "%04d", Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return value.trim();
        }
    }

    static String inferBuildingType(String mainPurpose, String etcPurpose, String buildingName) {
        String purpose = ((mainPurpose == null ? "" : mainPurpose) + " "
                + (etcPurpose == null ? "" : etcPurpose) + " "
                + (buildingName == null ? "" : buildingName)).replace(" ", "");

        if (purpose.contains("오피스텔")) return "OFFICETEL";
        if (purpose.contains("아파트")) return "APARTMENT";
        if (purpose.contains("다세대") || purpose.contains("연립") || purpose.contains("빌라")) {
            return "MULTI_HOUSEHOLD";
        }
        if (purpose.contains("다가구")) return "MULTI_FAMILY";
        if (purpose.contains("단독주택")) return "SINGLE_FAMILY";
        return null;
    }

    private int residentialScore(Map<String, Object> item) {
        String mainPurpose = (String) item.get("mainPurpsCdNm");
        String etcPurpose = (String) item.get("etcPurps");
        String buildingName = (String) item.get("bldNm");

        int score = inferBuildingType(mainPurpose, etcPurpose, buildingName) != null ? 100 : 0;
        if (mainPurpose != null && (mainPurpose.contains("공동주택") || mainPurpose.contains("단독주택"))) {
            score += 20;
        }
        if (etcPurpose != null && etcPurpose.contains("주거")) {
            score += 10;
        }
        if ("주건축물".equals(item.get("mainAtchGbCdNm"))) {
            score += 5;
        }
        return score;
    }

    static Boolean parseNullableYn(String value) {
        if (value == null || value.isBlank()) return null;
        if ("Y".equalsIgnoreCase(value.trim())) return true;
        if ("N".equalsIgnoreCase(value.trim())) return false;
        return null;
    }
}
