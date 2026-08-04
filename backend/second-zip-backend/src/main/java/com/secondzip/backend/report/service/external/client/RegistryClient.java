package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTarget;
import com.secondzip.backend.report.dto.external.RegistryData;
import com.secondzip.backend.report.enums.RegistryDocumentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CODEF 부동산등기부등본 열람 API 클라이언트.
 *
 * 이 API는 connectedId(계좌 연동) 방식이 아니라 매 요청에 organization/phoneNo/password를
 * 직접 실어 보내는 방식이라 별도의 계정 등록 절차가 없다.
 *
 * 응답은 "필드"가 아니라 등기부등본을 줄글로 잘라놓은 텍스트 구조라, 우리 쪽에서
 * 정규식으로 근저당/압류/신탁/소유자 여부를 추출해야 한다.
 *
 * 실패/파싱불가 시 항상 null 반환 (Mock 데이터 생성 금지 원칙 유지).
 */
@Slf4j
@Component
@org.springframework.context.annotation.Conditional(com.secondzip.backend.report.service.external.RealApiCondition.class)
public class RegistryClient implements RegistryDataProvider {

    private final RestTemplate restTemplate;
    private final CodefTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final RegistryRequestFactory requestFactory;
    private final RegistryDataParser registryDataParser;
    private final Map<String, CacheEntry> responseCache = new ConcurrentHashMap<>();

    @Value("${CODEF_REGISTRY_ENABLED:false}")
    private boolean registryEnabled;

    @Value("${CODEF_REGISTRY_CACHE_TTL_SECONDS:300}")
    private long cacheTtlSeconds;

    public RegistryClient(
            @Qualifier("codefRestTemplate") RestTemplate restTemplate,
            CodefTokenProvider tokenProvider,
            ObjectMapper objectMapper,
            RegistryRequestFactory requestFactory,
            RegistryDataParser registryDataParser
    ) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.requestFactory = requestFactory;
        this.registryDataParser = registryDataParser;
    }

    private record CacheEntry(RegistryData data, long expiresAtEpochMillis) {}

    // CODEF 등기부등본
    @Value("${CODEF_PUBLIC_KEY:}")
    private String publicKey;

    @Value("${CODEF_LOGIN_PHONE_NO:}")
    private String loginPhoneNo;

    @Value("${CODEF_LOGIN_PASSWORD:}")
    private String loginPassword;

    // 전자민원캐시 : 등기부등본 유료 발급
    @Value("${CODEF_EPREPAY_NO:}")
    private String ePrepayNo;

    @Value("${CODEF_EPREPAY_PASS:}")
    private String ePrepayPass;

    @Value("${CODEF_REGISTRY_BASE_URL:https://development.codef.io}")
    private String registryBaseUrl;

    private static final String REGISTRY_PATH =
            "/v1/kr/public/ck/real-estate-register/status";

    private static final Pattern MORTGAGE_AMOUNT_PATTERN =
            Pattern.compile("채권최고액\\s*금?\\s*([0-9,]+)\\s*원");
    private static final Pattern OWNER_NAME_PATTERN =
            Pattern.compile("소유자\\s+([가-힣]{2,5})");
    // 시도 약칭 → 공식 명칭 매핑 (roadAddress가 "서울", "경기" 같은 약칭으로 오는 경우 대비)
    private static final Map<String, String> SIDO_FULL_NAME = Map.ofEntries(
            Map.entry("서울", "서울특별시"),
            Map.entry("부산", "부산광역시"),
            Map.entry("대구", "대구광역시"),
            Map.entry("인천", "인천광역시"),
            Map.entry("광주", "광주광역시"),
            Map.entry("대전", "대전광역시"),
            Map.entry("울산", "울산광역시"),
            Map.entry("세종", "세종특별자치시"),
            Map.entry("경기", "경기도"),
            Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"),
            Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"),
            Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"),
            Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );

    /** roadAddress("서울 강남구 테헤란로 152")를 시도/시군구/도로명으로 분리 */
    private ParsedAddress parseRoadAddress(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) return null;

        String[] tokens = roadAddress.trim().split("\\s+");
        if (tokens.length < 3) {
            log.warn("roadAddress 토큰이 부족해 시군구/도로명 분리를 할 수 없습니다: {}", roadAddress);
            return null;
        }

        // 도로명(...로/...길)이 나오는 인덱스를 찾는다
        int roadIndex = -1;
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].endsWith("로") || tokens[i].endsWith("길")) {
                roadIndex = i;
                break;
            }
        }
        if (roadIndex == -1 || roadIndex == tokens.length - 1) {
            log.warn("roadAddress에서 도로명을 찾지 못했습니다: {}", roadAddress);
            return null;
        }

        String sido = tokens[0];
        String sidoFull = SIDO_FULL_NAME.getOrDefault(sido, sido);
        String sigungu = String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, roadIndex));
        String roadName = tokens[roadIndex];

        return new ParsedAddress(sidoFull, sigungu, roadName);
    }

    private record ParsedAddress(String sido, String sigungu, String roadName) {}

    /** "101동 101호" / "101동" / "101호" 등을 dong/ho로 분리 */
    private DongHo parseDongHo(String detailAddress) {
        if (detailAddress == null || detailAddress.isBlank()) return null;

        String dong = null;
        String ho = null;

        Matcher dongMatcher = Pattern.compile("(\\d+)\\s*동").matcher(detailAddress);
        if (dongMatcher.find()) dong = dongMatcher.group(1);

        Matcher hoMatcher = Pattern.compile("(\\d+)\\s*호").matcher(detailAddress);
        if (hoMatcher.find()) ho = hoMatcher.group(1);

        if (dong == null && ho == null) return null;
        return new DongHo(dong, ho);
    }

    private record DongHo(String dong, String ho) {}

    /** 기존 호출부 호환용 집합건물 등기부 조회 */
    public RegistryData getRegistryData(
            AnalysisTarget target,
            String detailAddress
    ) {
        return getRegistryData(
                target,
                detailAddress,
                RegistryDocumentType.COLLECTIVE
        );
    }

    public RegistryData getRegistryDataForAnalysis(
            AnalysisTarget target,
            String detailAddress,
            String buildingType
    ) {
        if ("SINGLE_FAMILY".equals(buildingType)
                || "MULTI_FAMILY".equals(buildingType)) {
            RegistryData building = getRegistryData(
                    target,
                    null,
                    RegistryDocumentType.BUILDING
            );
            RegistryData land = getRegistryData(
                    target,
                    null,
                    RegistryDocumentType.LAND
            );
            if (building == null && land == null) {
                return null;
            }
            RegistryData combined =
                    building == null ? new RegistryData() : building;
            if (land != null) {
                combined.setLandOwnerName(land.getOwnerName());
            }
            return combined;
        }
        return getRegistryData(
                target,
                detailAddress,
                RegistryDocumentType.COLLECTIVE
        );
    }

    /** 문서 종류를 명시한 등기부등본 데이터 조회 */
    public synchronized RegistryData getRegistryData(
            AnalysisTarget target,
            String detailAddress,
            RegistryDocumentType documentType
    ) {
        if (!registryEnabled) {
            log.info("CODEF 등기부등본 조회가 비활성화되어 있습니다.");
            return null;
        }
        if (target == null) {
            log.warn("AnalysisTarget이 null이라 등기부등본 조회를 스킵합니다.");
            return null;
        }
        if (loginPhoneNo == null || loginPhoneNo.isBlank()
                || loginPassword == null || loginPassword.isBlank()
                || publicKey == null || publicKey.isBlank()
                || ePrepayNo == null || ePrepayNo.isBlank()
                || ePrepayPass == null || ePrepayPass.isBlank()) {
            log.warn("CODEF 로그인/결제 정보가 없어 등기부등본 조회를 스킵합니다.");
            return null;
        }

        String cacheKey = documentType + "|" + target.roadAddress() + "|"
                + target.mainNo() + "|" + target.subNo() + "|"
                + (detailAddress == null ? "" : detailAddress);
        CacheEntry cached = responseCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < cached.expiresAtEpochMillis()) {
            log.info("CODEF 등기부등본 캐시 사용");
            return cached.data();
        }
        responseCache.remove(cacheKey);

        String token = tokenProvider.getToken();
        if (token == null) {
            log.warn("CODEF 토큰이 없어 등기부등본 조회를 스킵합니다.");
            return null;
        }

        String encryptedPassword = CodefRsaEncryptor.encrypt(loginPassword, publicKey);
        if (encryptedPassword == null) {
            log.warn("비밀번호 RSA 암호화 실패");
            return null;
        }

        try {
            Map<String, Object> requestBody = requestFactory.create(
                    target,
                    documentType,
                    detailAddress,
                    loginPhoneNo,
                    encryptedPassword,
                    ePrepayNo,
                    ePrepayPass
            );

            log.info("CODEF 등기부등본 조회 요청");
            ResponseEntity<String> response = postWithTokenRetry(requestBody, token);

            String rawBody = response.getBody();
            if (rawBody == null || rawBody.isBlank()) {
                log.warn("CODEF 등기부등본 응답이 비어있습니다.");
                return null;
            }

            try {
                Map<String, Object> body = parseResponseBody(rawBody);

                Map<String, Object> result = asMap(body.get("result"));
                String code = result != null && result.get("code") != null
                        ? result.get("code").toString()
                        : null;

                if ("CF-03002".equals(code)) {
                    log.warn("CODEF 추가인증이 필요해 조회를 중단합니다.");
                    return null;
                }
                if (code != null && !code.startsWith("CF-0000")) {
                    log.warn("CODEF 등기부등본 응답 에러: code={}", code);
                    return null;
                }

                Map<String, Object> data = asMap(body.get("data"));
                if (data == null) {
                    log.warn("CODEF 등기부등본 응답에 data가 없습니다.");
                    return null;
                }

                RegistryData registryData = registryDataParser.parse(data);
                if (registryData == null) {
                    log.warn("CODEF 등기부 응답 구조를 해석하지 못했습니다: shape={}", describeShape(data, 0));
                    return null;
                }

                long ttlMillis = Math.max(1L, cacheTtlSeconds) * 1000L;
                responseCache.put(
                        cacheKey,
                        new CacheEntry(registryData, System.currentTimeMillis() + ttlMillis)
                );
                return registryData;
            } catch (Exception e) {
                log.error("CODEF 등기부등본 응답 파싱 실패: type={}",
                        e.getClass().getSimpleName());
                return null;
            }

        } catch (Exception e) {
            log.error("CODEF 등기부등본 조회 실패: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    private String joinBuildingNumber(String mainNo, String subNo) {
        if (mainNo == null || mainNo.isBlank()) {
            return null;
        }
        return subNo == null || subNo.isBlank() || "0".equals(subNo)
                ? mainNo
                : mainNo + "-" + subNo;
    }

    private ResponseEntity<String> postWithTokenRetry(
            Map<String, Object> requestBody,
            String token
    ) {
        try {
            return postRegistryRequest(requestBody, token);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() != 401) {
                throw e;
            }
            tokenProvider.invalidate();
            String refreshedToken = tokenProvider.getToken();
            if (refreshedToken == null) {
                throw e;
            }
            log.info("CODEF 토큰 갱신 후 등기부 요청을 1회 재시도합니다.");
            return postRegistryRequest(requestBody, refreshedToken);
        }
    }

    private ResponseEntity<String> postRegistryRequest(
            Map<String, Object> requestBody,
            String token
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForEntity(
                registryBaseUrl + REGISTRY_PATH,
                request,
                String.class
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponseBody(String rawBody) throws Exception {
        try {
            return objectMapper.readValue(rawBody, Map.class);
        } catch (Exception plainJsonFailure) {
            String decoded = URLDecoder.decode(rawBody, StandardCharsets.UTF_8);
            return objectMapper.readValue(decoded, Map.class);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private RegistryData parseRegistryData(Map<String, Object> data) {
        List<Map<String, Object>> entriesList = new ArrayList<>();
        collectMapsFromNamedList(data, "resRegisterEntriesList", entriesList);
        if (entriesList == null || entriesList.isEmpty()) {
            log.warn("등기사항 목록이 비어있습니다.");
            return null;
        }

        String fullText = extractAllText(entriesList);
        if (fullText.isBlank()) {
            log.warn("등기사항 목록에 분석 가능한 텍스트가 없습니다.");
            return null;
        }

        Long mortgageAmount = extractMortgageAmount(fullText);
        Boolean hasSeizure = containsActiveKeyword(fullText, "압류", "가압류", "경매개시결정");
        Boolean hasTrustRegistration = containsActiveKeyword(fullText, "신탁");
        String ownerName = extractOwnerName(fullText);
        String ownerType = inferOwnerType(ownerName);

        RegistryData registryData = new RegistryData();
        registryData.setMortgageAmount(mortgageAmount);
        registryData.setHasSeizure(hasSeizure);
        registryData.setHasTrustRegistration(hasTrustRegistration);
        registryData.setOwnerName(ownerName);
        registryData.setOwnerType(ownerType);
        // landOwnerName, hasPostTrustInfringement: 토지 별도조회/시계열 비교 필요 — 아직 미구현(null 고정)

        log.info("등기부등본 파싱 완료: mortgageAmount={}, hasSeizure={}, hasTrustRegistration={}, ownerType={}",
                mortgageAmount, hasSeizure, hasTrustRegistration, ownerType);

        return registryData;
    }

    private void collectMapsFromNamedList(
            Object node,
            String targetKey,
            List<Map<String, Object>> destination
    ) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (targetKey.equals(String.valueOf(entry.getKey()))
                        && value instanceof Collection<?> collection) {
                    for (Object item : collection) {
                        Map<String, Object> itemMap = asMap(item);
                        if (itemMap != null) {
                            destination.add(itemMap);
                        }
                    }
                } else {
                    collectMapsFromNamedList(value, targetKey, destination);
                }
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(item -> collectMapsFromNamedList(item, targetKey, destination));
        }
    }

    private String extractAllText(Object node) {
        StringBuilder sb = new StringBuilder();
        appendTextValues(node, sb);
        return sb.toString();
    }

    private void appendTextValues(Object node, StringBuilder destination) {
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(value -> appendTextValues(value, destination));
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(value -> appendTextValues(value, destination));
        } else if (node instanceof CharSequence text && !text.toString().isBlank()) {
            destination.append(text).append('\n');
        }
    }

    private String describeShape(Object node, int depth) {
        if (node == null) {
            return "null";
        }
        if (depth >= 3) {
            return node.getClass().getSimpleName();
        }
        if (node instanceof Map<?, ?> map) {
            Map<String, String> shape = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count++ >= 30) {
                    shape.put("...", "truncated");
                    break;
                }
                shape.put(
                        String.valueOf(entry.getKey()),
                        describeShape(entry.getValue(), depth + 1)
                );
            }
            return shape.toString();
        }
        if (node instanceof Collection<?> collection) {
            Object first = collection.stream().findFirst().orElse(null);
            return "List(size=" + collection.size() + ", item="
                    + describeShape(first, depth + 1) + ")";
        }
        return node.getClass().getSimpleName();
    }

    private Long extractMortgageAmount(String text) {
        long total = 0L;
        boolean found = false;
        Matcher matcher = MORTGAGE_AMOUNT_PATTERN.matcher(text);
        while (matcher.find()) {
            // 문서 특이사항: 말소(취소선) 항목은 원본 텍스트에서 &...& 로 감싸져 온다.
            // 매치 위치가 & 쌍 안에 있으면 말소된 근저당이므로 합산에서 제외.
            if (isWithinCancelledSpan(text, matcher.start())) {
                continue;
            }
            try {
                total += Long.parseLong(matcher.group(1).replace(",", ""));
                found = true;
            } catch (NumberFormatException ignored) {
            }
        }
        return found ? total : 0L;
    }

    private boolean isWithinCancelledSpan(String text, int position) {
        int before = text.lastIndexOf('&', position);
        if (before == -1) return false;
        int after = text.indexOf('&', position);
        if (after == -1) return false;
        // 직전 & 이후 다음 &가 등장하기 전까지의 구간에 있으면 취소선(말소) 구간으로 판단
        return before < position && position < after;
    }

    private Boolean containsActiveKeyword(String text, String... keywords) {
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            while (idx != -1) {
                if (!isWithinCancelledSpan(text, idx)) {
                    return true;
                }
                idx = text.indexOf(keyword, idx + 1);
            }
        }
        return false;
    }

    private String extractOwnerName(String text) {
        Matcher matcher = OWNER_NAME_PATTERN.matcher(text);
        String lastOwner = null;
        while (matcher.find()) {
            if (!isWithinCancelledSpan(text, matcher.start())) {
                lastOwner = matcher.group(1); // 최신 소유자로 계속 갱신
            }
        }
        return lastOwner;
    }

    private String inferOwnerType(String ownerName) {
        if (ownerName == null) return null;
        return ownerName.contains("신탁") ? "TRUST_COMPANY" : "INDIVIDUAL";
    }
}
