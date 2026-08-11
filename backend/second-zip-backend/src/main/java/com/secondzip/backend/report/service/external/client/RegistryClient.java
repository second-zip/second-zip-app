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

        // 문서 종류가 키에 포함된다. 단독·다가구는 BUILDING과 LAND가 서로 다른 문서라
        // 각각 별도로 캐시된다.
        String cacheKey = documentType + "|" + target.roadAddress() + "|"
                + target.mainNo() + "|" + target.subNo() + "|"
                + (detailAddress == null ? "" : detailAddress);
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

                RegistryData registryData = registryDataParser.parse(data);
                if (registryData == null) {
                    log.warn("CODEF 등기부 응답 구조를 해석하지 못했습니다: shape={}", describeShape(data, 0));
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
