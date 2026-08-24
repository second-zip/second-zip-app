package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final RegistryDataCache registryDataCache;

    @Value("${CODEF_REGISTRY_ENABLED:false}")
    private boolean registryEnabled;

    public RegistryClient(
            @Qualifier("codefRestTemplate") RestTemplate restTemplate,
            CodefTokenProvider tokenProvider,
            ObjectMapper objectMapper,
            RegistryRequestFactory requestFactory,
            RegistryDataParser registryDataParser,
            RegistryDataCache registryDataCache
    ) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.requestFactory = requestFactory;
        this.registryDataParser = registryDataParser;
        this.registryDataCache = registryDataCache;
    }

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

    /** 기존 호출부 호환용 집합건물 등기부 조회 */
    public RegistryData getRegistryData(
            AnalysisTargetDTO target,
            String detailAddress
    ) {
        return getRegistryData(
                target,
                detailAddress,
                RegistryDocumentType.COLLECTIVE
        );
    }

    public RegistryData getRegistryDataForAnalysis(
            AnalysisTargetDTO target,
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
            if (building == null) {
                // 건물 등기부 없이는 어차피 완성된 분석을 만들 수 없다. 유료 토지
                // 조회까지 이어가 불필요한 과금을 만들지 않는다.
                log.warn("건물 등기부 조회 실패로 토지 등기부 조회를 생략합니다.");
                return null;
            }
            RegistryData land = getRegistryData(
                    target,
                    null,
                    RegistryDocumentType.LAND
            );
            // 두 문서는 한 물건의 권리관계를 함께 이룬다. 한쪽만 성공한 값을
            // 완성된 분석처럼 반환하면 누락된 담보/압류를 0 또는 false로 확정한다.
            if (land == null) {
                log.warn(
                        "단독·다가구 등기부가 부분 조회되어 분석을 중단합니다: building={}, land={}",
                        true,
                        false
                );
                return null;
            }
            return combineBuildingAndLand(building, land);
        }
        return getRegistryData(
                target,
                detailAddress,
                RegistryDocumentType.COLLECTIVE
        );
    }

    private RegistryData combineBuildingAndLand(
            RegistryData building,
            RegistryData land
    ) {
        RegistryData combined = new RegistryData();
        combined.setMortgageAmount(mergeVerifiedAmounts(
                building.getMortgageAmount(),
                land.getMortgageAmount()
        ));
        combined.setHasSeizure(orUnknown(
                building.getHasSeizure(),
                land.getHasSeizure()
        ));
        combined.setHasTrustRegistration(orUnknown(
                building.getHasTrustRegistration(),
                land.getHasTrustRegistration()
        ));
        combined.setHasPostTrustInfringement(orUnknown(
                building.getHasPostTrustInfringement(),
                land.getHasPostTrustInfringement()
        ));

        combined.setOwnerName(building.getOwnerName());
        combined.setOwnerNames(copyList(building.getOwnerNames()));
        combined.setOwnerType(building.getOwnerType());
        combined.setLandOwnerName(land.getOwnerName());
        combined.setLandOwnerNames(copyList(land.getOwnerNames()));
        combined.setLandOwnerType(land.getOwnerType());
        return combined;
    }

    private Long mergeVerifiedAmounts(Long buildingAmount, Long landAmount) {
        if (buildingAmount == null || landAmount == null) {
            return null;
        }
        if (buildingAmount < 0L || landAmount < 0L) {
            return null;
        }
        if (buildingAmount == 0L) {
            return landAmount;
        }
        if (landAmount == 0L) {
            return buildingAmount;
        }
        // 단독·다가구의 근저당은 거의 예외 없이 건물·토지 공동담보로 잡히고,
        // 그때 양쪽 등기에 같은 채권최고액이 기재된다. 이 경우까지 미확인으로
        // 보내면 근저당이 있는 단독·다가구 대부분에서 핵심 금액이 사라진다.
        if (buildingAmount.equals(landAmount)) {
            log.info("건물·토지 채권최고액이 같아 공동담보로 보고 한 건으로 집계합니다.");
            return buildingAmount;
        }
        // 금액이 다르면 일부만 공동담보인지 각각 별도 근저당인지 구분할 근거가
        // 없다. 합산해 과대평가하지도, 큰 쪽만 골라 과소평가하지도 않는다.
        log.warn("건물·토지 채권최고액이 달라 근저당 금액을 확정하지 않습니다.");
        return null;
    }

    private Boolean orUnknown(Boolean first, Boolean second) {
        if (Boolean.TRUE.equals(first) || Boolean.TRUE.equals(second)) {
            return true;
        }
        if (Boolean.FALSE.equals(first) && Boolean.FALSE.equals(second)) {
            return false;
        }
        return null;
    }

    private List<String> copyList(List<String> values) {
        return values == null ? null : List.copyOf(values);
    }

    /** 문서 종류를 명시한 등기부등본 데이터 조회 */
    public synchronized RegistryData getRegistryData(
            AnalysisTargetDTO target,
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

        // 문서 종류가 키에 포함된다. 단독·다가구는 BUILDING과 LAND가 서로 다른 문서라
        // 각각 별도로 캐시된다.
        String cacheKey = documentType + "|" + cachePart(target.roadAddress()) + "|"
                + cachePart(target.legalDongName()) + "|"
                + cachePart(target.platGbCd()) + "|"
                + cachePart(target.mainNo()) + "|"
                + cachePart(target.subNo()) + "|"
                + cachePart(target.lotAddress()) + "|"
                + cachePart(detailAddress);
        RegistryData cached = findCached(cacheKey);
        if (cached != null) {
            return cached;
        }

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
                String message = asText(result, "message");
                String extraMessage = asText(result, "extraMessage");

                if ("CF-03002".equals(code)) {
                    log.warn(
                            "CODEF 추가인증이 필요해 조회를 중단합니다. message={}, extraMessage={}",
                            message,
                            extraMessage
                    );
                    return null;
                }
                if (code != null && !code.startsWith("CF-0000")) {
                    log.warn(
                            "CODEF 등기부등본 응답 에러: code={}, message={}, extraMessage={}",
                            code,
                            message,
                            extraMessage
                    );
                    return null;
                }

                Map<String, Object> data = asMap(body.get("data"));
                if (data == null) {
                    log.warn(
                            "CODEF 등기부등본 응답에 data가 없습니다. code={}, message={}",
                            code,
                            message
                    );
                    return null;
                }

                RegistryData registryData = registryDataParser.parse(
                        data,
                        documentType == RegistryDocumentType.COLLECTIVE
                                ? detailAddress
                                : null
                );
                if (registryData == null) {
                    logUnparseableResponse(
                            data, detailAddress, requestBody, target.legalDongName());
                    return null;
                }

                // 과금이 끝난 결과이므로 반드시 캐시에 남긴다.
                putCached(cacheKey, registryData);
                return registryData;
            } catch (Exception e) {
                log.error("CODEF 등기부등본 응답 파싱 실패: type={}",
                        e.getClass().getSimpleName());
                return null;
            }

        } catch (Exception e) {
            log.error(
                    "CODEF 등기부등본 조회 실패: type={}, message={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            return null;
        }
    }

    /**
     * 캐시 조회. 캐시는 최적화일 뿐이므로 어떤 이유로도 조회 자체를 막지 않는다.
     * 캐시가 주입되지 않은 환경(단위 테스트 등)도 허용한다.
     */
    private RegistryData findCached(String cacheKey) {
        if (registryDataCache == null) {
            return null;
        }
        try {
            return registryDataCache.find(cacheKey);
        } catch (Exception e) {
            log.warn("등기부등본 캐시 조회를 건너뜁니다: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    private String cachePart(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 캐시 저장. 이 시점엔 이미 과금이 끝났으므로,
     * 저장에 실패하더라도 절대 결과를 잃어버리면 안 된다.
     */
    private void putCached(String cacheKey, RegistryData data) {
        if (registryDataCache == null) {
            return;
        }
        try {
            registryDataCache.put(cacheKey, data);
        } catch (Exception e) {
            log.warn("등기부등본 캐시 저장을 건너뜁니다: type={}", e.getClass().getSimpleName());
        }
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

    /**
     * result 하위의 진단용 문자열 필드를 꺼낸다.
     * CODEF는 실패 사유를 code가 아니라 message/extraMessage에 담아 보내고,
     * 한글이 퍼센트 인코딩된 채로 오는 경우가 있어 필요할 때만 디코딩한다.
     */
    private String asText(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (text.indexOf('%') < 0) {
            return text;
        }
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return text;
        }
    }

    /**
     * registryDataParser.parse()가 null을 반환했을 때 원인을 구분해서 남긴다.
     *
     * resRegisterEntriesList가 비어있고 resAddrList만 채워진 경우는 진짜 파싱
     * 실패가 아니라, CODEF(등기소)가 요청한 동/호로 등기부를 하나로 특정하지
     * 못해 주소 후보 목록을 대신 내려준 것으로 보인다. 이 경우 실제로 무엇을
     * 보냈는지(원문 상세주소, 파싱된 dong/ho)와 후보가 몇 건/어떤 지번인지를
     * 남겨야 다음에 같은 상황이 재현될 때 추측 없이 원인을 확인할 수 있다.
     */
    private void logUnparseableResponse(
            Map<String, Object> data,
            String detailAddress,
            Map<String, Object> requestBody,
            String legalDongName
    ) {
        List<Map<String, Object>> registerEntries = new ArrayList<>();
        collectMapsFromNamedList(data, "resRegisterEntriesList", registerEntries);
        List<Map<String, Object>> addressCandidates = new ArrayList<>();
        collectMapsFromNamedList(data, "resAddrList", addressCandidates);

        if (registerEntries.isEmpty() && !addressCandidates.isEmpty()) {
            // 동 없이 호수만 요청하면, 이 건물이 실제로 동으로 세대를 나누는 경우
            // (숫자 동이든 "제비동"처럼 이름 붙은 동이든) 등기소가 후보를 하나로
            // 좁히지 못한다. 이때 이 건물이 실제로 쓰는 동 이름을 후보 지번에서
            // 뽑아 남겨두면, 사용자가 상세주소에 어떤 동을 채워 넣어야 하는지
            // 추측 없이 바로 알 수 있다.
            log.warn(
                    "CODEF가 등기부를 하나로 특정하지 못해 주소 후보 목록을 대신 "
                            + "반환했습니다: detailAddress={}, 요청 dong={}, ho={}, "
                            + "후보 수={}, 이 건물이 쓰는 동 이름={}, 후보 지번 예시={}",
                    detailAddress,
                    requestBody.get("dong"),
                    requestBody.get("ho"),
                    addressCandidates.size(),
                    distinctDongLabels(addressCandidates, legalDongName),
                    distinctLotNumbers(addressCandidates)
            );
            return;
        }
        log.warn("CODEF 등기부 응답 구조를 해석하지 못했습니다: shape={}", describeShape(data, 0));
    }

    private static final java.util.regex.Pattern ADDR_DONG_TOKEN =
            java.util.regex.Pattern.compile("([^\\s,()]+?)동(?=[0-9])");

    /**
     * 후보 지번 문자열(예: "...백현동 529 판교역에스케이허브 제1층 제비동101호")에서
     * 실제 세대 구분에 쓰이는 동 이름만 뽑는다. "동" 바로 뒤에 공백 없이 숫자가
     * 붙어야만 세대 동으로 보고, 법정동명(예: "백현동")은 뒤에 지번 숫자가 아니라
     * 공백이 오므로 이 조건만으로도 자연히 걸러진다 — legalDongName은 혹시 공백
     * 없이 붙어 나오는 응답 형식을 대비한 이중 방어다.
     */
    private Set<String> distinctDongLabels(
            List<Map<String, Object>> candidates, String legalDongName
    ) {
        String excluded = legalDongName == null
                ? null
                : legalDongName.trim().replaceAll("\\s+", "");
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> candidate : candidates) {
            Object value = candidate.get("commAddrLotNumber");
            if (value == null) {
                continue;
            }
            java.util.regex.Matcher matcher = ADDR_DONG_TOKEN.matcher(value.toString());
            while (matcher.find()) {
                String token = matcher.group(1) + "동";
                if (excluded != null && excluded.equals(token)) {
                    continue;
                }
                result.add(token);
            }
            if (result.size() >= 20) {
                result.add("...");
                break;
            }
        }
        return result;
    }

    private Set<String> distinctLotNumbers(List<Map<String, Object>> candidates) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> candidate : candidates) {
            Object value = candidate.get("commAddrLotNumber");
            if (value != null && !value.toString().isBlank()) {
                result.add(value.toString());
            }
            if (result.size() >= 10) {
                result.add("...");
                break;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
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
                        if (item instanceof Map<?, ?> itemMap) {
                            destination.add((Map<String, Object>) itemMap);
                        }
                    }
                } else {
                    collectMapsFromNamedList(value, targetKey, destination);
                }
            }
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(item -> collectMapsFromNamedList(
                    item, targetKey, destination));
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

}
