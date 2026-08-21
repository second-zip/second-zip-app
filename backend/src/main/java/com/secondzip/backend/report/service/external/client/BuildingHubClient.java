package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import com.secondzip.backend.report.dto.external.BuildingData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 건축HUB 건축물대장정보 서비스 (BldRgstHubService) - 표제부 조회(getBrTitleInfo) 클라이언트.
 * AnalysisTarget(주소 표준화 결과)을 받아 건축물 용도, 위반건축물 여부 등을 조회.
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
    private static final int ROWS_PER_PAGE = 100;
    private static final int MAX_PAGES = 100;

    public BuildingData getBuildingData(AnalysisTargetDTO target) {
        return getBuildingData(target, null);
    }

    public BuildingData getBuildingData(AnalysisTargetDTO target, String detailAddress) {
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
            List<Map<String, Object>> allItems = new ArrayList<>();
            int totalCount = Integer.MAX_VALUE;
            Integer declaredTotalCount = null;
            for (int pageNo = 1;
                 pageNo <= MAX_PAGES && allItems.size() < totalCount;
                 pageNo++) {
                HubPage page = fetchPage(target, pageNo);
                if (page == null) return null;
                if (declaredTotalCount != null
                        && declaredTotalCount != page.totalCount()) {
                    log.warn(
                            "건축HUB 페이지별 totalCount가 다릅니다: first={}, current={}",
                            declaredTotalCount,
                            page.totalCount()
                    );
                    return null;
                }
                declaredTotalCount = page.totalCount();
                allItems.addAll(page.items());
                totalCount = page.totalCount();
                if (page.items().isEmpty()) break;
            }
            if (allItems.size() != totalCount) {
                log.warn("건축HUB 수집 건수와 totalCount가 다릅니다: collected={}, totalCount={}",
                        allItems.size(), totalCount);
                return null;
            }

            Map<String, Object> item = selectTargetItem(
                    allItems,
                    target,
                    detailAddress
            );
            if (item == null) {
                log.warn("건축물대장 표제부 조회 결과가 없습니다.");
                return null;
            }


            String mainPurpsCdNm = (String) item.get("mainPurpsCdNm"); // 주용도코드명
            String etcPurps = (String) item.get("etcPurps");           // 기타용도
            String buildingName = (String) item.get("bldNm");          // 건물명
            String violBldgYn = (String) item.get("violBldgYn");       // 공식 문서엔 없어서 방어적으로 확인

            BuildingData data = new BuildingData();
            data.setBuildingUse(combineBuildingUse(mainPurpsCdNm, etcPurps));
            data.setBuildingType(inferBuildingType(mainPurpsCdNm, etcPurps, buildingName));
            data.setIsIllegalBuilding(parseNullableYn(violBldgYn));
            data.setTransactionAreaSqm(parsePositiveDecimal(item.get("totArea")));

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
    private HubPage fetchPage(AnalysisTargetDTO target, int pageNo) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiKey)
                .queryParam("sigunguCd", target.sigunguCode())
                .queryParam("bjdongCd", target.bjdongCode());
        if (hasText(target.platGbCd())) {
            uriBuilder.queryParam("platGbCd", target.platGbCd());
        }
        URI uri = uriBuilder
                .queryParam("bun", normalizeLotNumber(target.mainNo()))
                .queryParam("ji", normalizeLotNumber(target.subNo()))
                .queryParam("numOfRows", ROWS_PER_PAGE)
                .queryParam("pageNo", pageNo)
                .queryParam("_type", "json")
                .build(true)
                .toUri();

        log.info("건축HUB 조회 요청: pageNo={}", pageNo);
        ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) {
            log.warn("건축HUB 응답이 비어있습니다.");
            return null;
        }
        Map<String, Object> responseMap = asMap(body.get("response"));
        if (responseMap == null) {
            log.warn("건축HUB 응답 형식이 예상과 다릅니다.");
            return null;
        }
        Map<String, Object> header = asMap(responseMap.get("header"));
        String resultCode = header != null ? stringValue(header.get("resultCode")) : null;
        if (!"00".equals(resultCode)) {
            log.warn("건축HUB 응답 에러: code={}, msg={}",
                    resultCode, header != null ? header.get("resultMsg") : null);
            return null;
        }
        Map<String, Object> bodyMap = asMap(responseMap.get("body"));
        if (bodyMap == null) {
            log.warn("건축HUB 응답에 body가 없습니다.");
            return null;
        }
        List<Map<String, Object>> items = extractItems(bodyMap);
        Integer totalCount = parseNonNegativeInt(bodyMap.get("totalCount"));
        if (totalCount == null || totalCount < items.size()) {
            log.warn("건축HUB totalCount가 없거나 유효하지 않습니다: totalCount={}",
                    bodyMap.get("totalCount"));
            return null;
        }
        return new HubPage(items, totalCount);
    }

    private Map<String, Object> selectTargetItem(
            List<Map<String, Object>> items,
            AnalysisTargetDTO target,
            String detailAddress
    ) {
        if (items == null || items.isEmpty()) return null;
        if (items.size() == 1) {
            IdentityScore only = identityScore(items.get(0), target, detailAddress);
            return only.conflict() ? null : items.get(0);
        }

        Map<String, Object> best = null;
        int bestScore = -1;
        boolean tied = false;
        for (Map<String, Object> item : items) {
            IdentityScore identity = identityScore(item, target, detailAddress);
            if (identity.matched() && !identity.conflict()) {
                if (identity.score() > bestScore) {
                    best = item;
                    bestScore = identity.score();
                    tied = false;
                } else if (identity.score() == bestScore) {
                    tied = true;
                }
            }
        }
        return tied ? null : best;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> bodyMap) {
        Map<String, Object> items = asMap(bodyMap.get("items"));
        if (items == null) return List.of();
        Object item = items.get("item");
        if (item instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object value : list) {
                Map<String, Object> map = asMap(value);
                if (map != null) result.add(map);
            }
            return result;
        }
        Map<String, Object> single = asMap(item);
        return single != null ? List.of(single) : List.of();
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
        TypeInference fromEtcPurpose = inferFromText(etcPurpose, false);
        if (fromEtcPurpose.recognized()) return fromEtcPurpose.type();

        TypeInference fromMainPurpose = inferFromText(mainPurpose, false);
        if (fromMainPurpose.recognized()) return fromMainPurpose.type();

        return inferFromText(buildingName, true).type();
    }

    /** 하나의 권위 필드가 서로 다른 주택유형을 함께 가리키면 임의 우선순위를 두지 않는다. */
    private static TypeInference inferFromText(String raw, boolean allowInformalName) {
        if (raw == null || raw.isBlank()) return TypeInference.none();
        String value = raw.replaceAll("\\s+", "");
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (value.contains("오피스텔")) candidates.add("OFFICETEL");
        boolean multiFamily = value.contains("다가구");
        if (multiFamily) candidates.add("MULTI_FAMILY");
        if (value.contains("다세대") || value.contains("연립")
                || allowInformalName && value.contains("빌라")) {
            candidates.add("MULTI_HOUSEHOLD");
        }
        if (value.contains("아파트")) candidates.add("APARTMENT");
        // 다가구는 단독주택의 세부 유형이므로 같은 문자열의 '단독주택'은 충돌로 세지 않는다.
        if (!multiFamily && value.contains("단독주택")) {
            candidates.add("SINGLE_FAMILY");
        }
        if (candidates.isEmpty()) return TypeInference.none();
        return new TypeInference(
                true,
                candidates.size() == 1 ? candidates.iterator().next() : null
        );
    }

    static String combineBuildingUse(String mainPurpose, String etcPurpose) {
        String main = trimToNull(mainPurpose);
        String etc = trimToNull(etcPurpose);
        if (main == null) return etc;
        if (etc == null || normalizeIdentity(main).equals(normalizeIdentity(etc))) {
            return main;
        }
        return main + ", " + etc;
    }

    private IdentityScore identityScore(
            Map<String, Object> item,
            AnalysisTargetDTO target,
            String detailAddress
    ) {
        if (item == null || target == null) {
            return IdentityScore.none();
        }

        IdentityScore score = IdentityScore.none();
        score = compareIdentity(
                score,
                target.buildingManagementNo(),
                stringValue(item.get("mgmBldrgstPk")),
                10_000
        );
        score = compareIdentity(
                score,
                target.roadBuildingMainNo(),
                stringValue(item.get("naMainBun")),
                1_000
        );

        String targetRoadSub = zeroIfBlank(target.roadBuildingSubNo());
        String itemRoadSub = zeroIfBlank(stringValue(item.get("naSubBun")));
        if (hasText(target.roadBuildingMainNo())
                && hasText(stringValue(item.get("naMainBun")))
                && item.containsKey("naSubBun")) {
            score = compareIdentity(score, targetRoadSub, itemRoadSub, 100);
        }

        String targetDong = extractDetailToken(detailAddress, "동");
        score = compareUnitIdentity(
                score,
                targetDong,
                firstText(item, "dongNm", "resDong", "resDongNm"),
                2_000
        );

        String itemRoadAddress = firstText(item, "newPlatPlc", "newPlatPlcAddr");
        if (hasText(target.roadAddress()) && hasText(itemRoadAddress)
                && normalizeIdentity(target.roadAddress())
                .equals(normalizeIdentity(itemRoadAddress))) {
            score = score.addMatch(3_000);
        }
        return score;
    }

    private IdentityScore compareIdentity(
            IdentityScore current,
            String expected,
            String actual,
            int weight
    ) {
        if (!hasText(expected) || !hasText(actual)) return current;
        return normalizeIdentity(expected).equals(normalizeIdentity(actual))
                ? current.addMatch(weight)
                : current.addConflict();
    }

    private IdentityScore compareUnitIdentity(
            IdentityScore current,
            String expected,
            String actual,
            int weight
    ) {
        if (!hasText(expected) || !hasText(actual)) return current;
        return normalizeUnitIdentity(expected).equals(normalizeUnitIdentity(actual))
                ? current.addMatch(weight)
                : current.addConflict();
    }

    private static String normalizeUnitIdentity(String value) {
        String normalized = value.replaceAll("\\s+", "").trim();
        if (normalized.endsWith("동")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("제")) {
            normalized = normalized.substring(1);
        }
        return normalizeIdentity(normalized);
    }

    private static String extractDetailToken(String detailAddress, String suffix) {
        if (!hasText(detailAddress)) return null;
        Matcher matcher = Pattern.compile(
                "([0-9A-Za-z가-힣_-]+)\\s*" + Pattern.quote(suffix)
        ).matcher(detailAddress);
        String found = null;
        while (matcher.find()) {
            found = matcher.group(1) + suffix;
        }
        return found;
    }

    private static String firstText(Map<String, Object> item, String... keys) {
        for (String key : keys) {
            String value = stringValue(item.get(key));
            if (hasText(value)) return value;
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private static Integer parseNonNegativeInt(Object raw) {
        if (raw == null) return null;
        try {
            int value = Integer.parseInt(raw.toString().trim());
            return value >= 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static BigDecimal parsePositiveDecimal(Object raw) {
        if (raw == null) return null;
        Matcher matcher = Pattern.compile("[-+]?[0-9][0-9,]*(?:\\.[0-9]+)?")
                .matcher(raw.toString());
        if (!matcher.find()) return null;
        try {
            BigDecimal value = new BigDecimal(matcher.group().replace(",", ""));
            return value.signum() > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String zeroIfBlank(String value) {
        return hasText(value) ? value : "0";
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeIdentity(String value) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", "").trim();
        if (normalized.matches("[0-9]+")) {
            try {
                return Long.toString(Long.parseLong(normalized));
            } catch (NumberFormatException ignored) {
                return normalized;
            }
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    static Boolean parseNullableYn(String value) {
        if (value == null || value.isBlank()) return null;
        if ("Y".equalsIgnoreCase(value.trim())) return true;
        if ("N".equalsIgnoreCase(value.trim())) return false;
        return null;
    }

    private record IdentityScore(int score, boolean matched, boolean conflict) {
        static IdentityScore none() {
            return new IdentityScore(0, false, false);
        }

        IdentityScore addMatch(int amount) {
            return new IdentityScore(score + amount, true, conflict);
        }

        IdentityScore addConflict() {
            return new IdentityScore(score, matched, true);
        }
    }

    private record HubPage(List<Map<String, Object>> items, int totalCount) {
    }

    private record TypeInference(boolean recognized, String type) {
        static TypeInference none() {
            return new TypeInference(false, null);
        }
    }
}
