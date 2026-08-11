package com.secondzip.backend.report.service.external.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.report.dto.external.RegistryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 기반 등기부등본 캐시.
 *
 * <p><b>왜 인스턴스 로컬 맵이 아니라 Redis인가</b>
 * <ul>
 *   <li>서버를 재시작해도 캐시가 살아남는다.</li>
 *   <li>인스턴스를 늘려도(Scale Out) 캐시를 공유한다.
 *       로컬 맵이면 인스턴스마다 따로 채워져 그만큼 중복 과금이 발생한다.</li>
 * </ul>
 *
 * <p><b>TTL은 왜 30분인가</b>
 * <ul>
 *   <li><b>분 단위에서는 신선도가 사실상 문제되지 않는다.</b> 등기 변동은 신청 → 접수 → 완료까지
 *       며칠이 걸린다. 30분 안에 근저당이 새로 설정될 확률은 무시할 수준이다.</li>
 *   <li>워크플로 TTL(기본 15분)보다 길게 두어, <b>재시도할 때 다시 과금되는 구간</b>을 없앤다.
 *       캐시가 더 짧으면 "워크플로는 살아있는데 캐시만 죽은" 구간에서 재시도가 재과금된다.</li>
 *   <li>결과적으로 <b>"30분 내 같은 매물 = 등기 1회 과금"</b>이 보장된다.</li>
 * </ul>
 *
 * <p><b>여기서 더 늘리면 안 되는 이유</b> — 상한을 정하는 건 비용이 아니라 두 가지다.
 * <ul>
 *   <li>시간·일 단위로 가면 그때는 실제로 낡은 등기로 "안전"을 판정할 수 있다.
 *       전세사기 분석에서 이건 사용자의 실질적 피해다.</li>
 *   <li>{@code ownerName}, {@code landOwnerName}은 개인정보다.
 *       Redis에 남는 시간이 곧 보관 기간이다.</li>
 * </ul>
 */
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
