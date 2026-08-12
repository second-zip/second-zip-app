package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.AnalysisTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 주소 검색 결과를 잠시 보관하고 토큰으로 되찾는다.
 *
 * 사용자가 검색 화면에서 고른 바로 그 결과를 분석에 쓰기 위한 장치.
 * 이게 없으면 백엔드가 주소 문자열로 재검색해 첫 번째 결과를 쓰게 되고,
 * 사용자가 고른 것과 다른 건물이 분석될 수 있다.
 *
 * TTL 10분은 "검색 → 선택 → 보증금 입력 → 분석 시작"을 덮는 시간이다.
 * 워크플로 TTL(기본 900초)보다 짧게 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressSearchCache {

    private static final String KEY_PREFIX = "address:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 검색 결과를 저장하고 토큰을 반환한다.
     *
     * 실패해도 예외를 던지지 않는다. 토큰이 없으면 재검색 경로로 떨어질 뿐이고,
     * 그것 때문에 주소 검색 화면 전체가 죽으면 안 된다.
     *
     * 저장 실패 시 null
     */
    public String put(AnalysisTarget target) {
        String token = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token,
                    objectMapper.writeValueAsString(target),
                    TTL
            );
            return token;
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("주소 검색 결과 캐시 실패: {}", e.getMessage());
            return null;
        }
    }

    /** 토큰으로 조회한다. 없거나 만료됐으면 null. */
    public AnalysisTarget find(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + token);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, AnalysisTarget.class);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("주소 토큰 조회 실패: {}", e.getMessage());
            return null;
        }
    }
}