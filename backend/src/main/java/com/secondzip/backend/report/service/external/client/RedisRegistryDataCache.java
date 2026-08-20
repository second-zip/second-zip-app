package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.external.RegistryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 기반 등기부등본 캐시
 * */
@Slf4j
@Component
public class RedisRegistryDataCache implements RegistryDataCache {

    private static final String KEY_PREFIX = "registry:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${CODEF_REGISTRY_CACHE_TTL_SECONDS:300}")
    private long ttlSeconds;

    public RedisRegistryDataCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RegistryData find(String cacheKey) {
        if (redisTemplate == null || cacheKey == null) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            log.info("CODEF 등기부등본 캐시 사용");
            return objectMapper.readValue(json, RegistryData.class);
        } catch (Exception e) {
            // 캐시 조회 실패는 캐시 미스로 처리한다. 분석을 막지 않는다.
            log.warn(
                    "등기부등본 캐시 조회 실패: type={}, message={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            return null;
        }
    }

    @Override
    public void put(String cacheKey, RegistryData data) {
        if (redisTemplate == null || cacheKey == null || data == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + cacheKey,
                    objectMapper.writeValueAsString(data),
                    Duration.ofSeconds(Math.max(1L, ttlSeconds))
            );
        } catch (Exception e) {
            // 이미 과금은 끝난 시점이다. 캐시 저장에 실패해도 결과는 그대로 반환한다.
            log.warn(
                    "등기부등본 캐시 저장 실패: type={}, message={}",
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
        }
    }
}
