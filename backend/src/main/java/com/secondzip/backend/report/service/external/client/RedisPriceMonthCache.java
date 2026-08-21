package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Redis 기반 실거래가 월별 응답 캐시.
 *
 * 확정된 과거 달의 거래는 거의 변하지 않지만, 신고기한(계약 후 30일) 때문에
 * 최근 달은 계속 늘어난다. 그래서 TTL을 짧게 잡아 최신 달이 오래 굳지 않게 한다.
 */
@Slf4j
@Component
public class RedisPriceMonthCache implements PriceMonthCache {

    private static final String KEY_PREFIX = "price:month:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${REALTY_PRICE_CACHE_TTL_SECONDS:21600}")
    private long ttlSeconds;

    public RedisPriceMonthCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> find(String cacheKey) {
        if (redisTemplate == null || cacheKey == null) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(redisKey(cacheKey));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {
                    }
            );
        } catch (Exception e) {
            // 캐시 조회 실패는 캐시 미스로 처리한다. 분석을 막지 않는다.
            log.warn(
                    "실거래가 월별 캐시 조회 실패: type={}, message={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            return null;
        }
    }

    @Override
    public void put(String cacheKey, List<Map<String, Object>> items) {
        if (redisTemplate == null || cacheKey == null || items == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    redisKey(cacheKey),
                    objectMapper.writeValueAsString(items),
                    Duration.ofSeconds(Math.max(1L, ttlSeconds))
            );
        } catch (Exception e) {
            log.warn(
                    "실거래가 월별 캐시 저장 실패: type={}, message={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }

    /**
     * 캐시 키에는 serviceKey가 아닌 서비스 URL이 들어가지만, URL이 그대로
     * 노출되지 않도록 해시로 줄인다. 키 길이도 함께 짧아진다.
     */
    private String redisKey(String cacheKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(cacheKey.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            return KEY_PREFIX + Integer.toHexString(cacheKey.hashCode());
        }
    }
}
