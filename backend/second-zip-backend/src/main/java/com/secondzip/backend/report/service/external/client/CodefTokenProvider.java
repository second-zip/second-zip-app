package com.secondzip.backend.report.service.external.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * CODEF OAuth 토큰 발급/캐싱.
 * access_token은 약 1주일 유효 → 매 요청마다 새로 받지 않고 메모리에 캐싱해서 재사용한다.
 * 토큰 만료로 401이 나면 호출부에서 invalidate() 후 재시도하는 구조.
 */
@Slf4j
@Component
public class CodefTokenProvider {

    private final RestTemplate restTemplate;

    @Value("${CODEF_CLIENT_ID:}")
    private String clientId;

    @Value("${CODEF_CLIENT_SECRET:}")
    private String clientSecret;

    private static final String TOKEN_URL = "https://oauth.codef.io/oauth/token";

    private volatile String cachedToken;
    private volatile long expiresAtEpochMillis;

    public CodefTokenProvider(@Qualifier("codefRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public synchronized String getToken() {
        if (cachedToken != null && System.currentTimeMillis() < expiresAtEpochMillis) {
            return cachedToken;
        }
        cachedToken = requestNewToken();
        return cachedToken;
    }

    public synchronized void invalidate() {
        cachedToken = null;
        expiresAtEpochMillis = 0L;
    }

    private String requestNewToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.warn("CODEF 클라이언트 정보가 없습니다.");
            return null;
        }
        try {
            String basicAuth = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuth);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "read");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("access_token") == null) {
                log.warn("CODEF 토큰 발급 실패: access_token이 응답에 없습니다.");
                return null;
            }

            long expiresInSeconds = parseExpiresIn(responseBody.get("expires_in"));
            expiresAtEpochMillis = System.currentTimeMillis()
                    + Math.max(60L, expiresInSeconds - 60L) * 1000L;
            log.info("CODEF 토큰 발급 완료");
            return String.valueOf(responseBody.get("access_token"));

        } catch (Exception e) {
            log.error("CODEF 토큰 발급 중 에러", e);
            return null;
        }
    }

    private long parseExpiresIn(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
                // CODEF 기본 유효기간을 사용한다.
            }
        }
        return 7L * 24L * 60L * 60L;
    }
}
