package com.secondzip.backend.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.common.exception.ErrorCode;
import com.secondzip.backend.report.dto.AnalysisTargetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisAddressSearchStore implements AddressSearchStore {

    private static final String KEY_PREFIX = "address-search:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 보관 시간.
     *
     * 사용자가 주소를 고른 뒤 보증금을 입력하고 분석을 시작하기까지의 시간을 덮어야 한다.
     * 너무 짧으면 화면에 머무는 동안 만료돼 다시 검색하게 되고,
     * 너무 길면 쓰이지 않을 후보가 Redis에 오래 남는다.
     */
    @Value("${ADDRESS_SEARCH_TTL_SECONDS:1800}")
    private long ttlSeconds;

    @Override
    public String save(AnalysisTargetDTO target) {
        String addressId = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + addressId,
                    objectMapper.writeValueAsString(target),
                    Duration.ofSeconds(Math.max(60L, ttlSeconds))
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "주소 정보를 저장하지 못했습니다."
            );
        }
        return addressId;
    }

    @Override
    public AnalysisTargetDTO find(String addressId) {
        if (addressId == null || addressId.isBlank()) {
            throw expired();
        }

        String json = redisTemplate.opsForValue().get(KEY_PREFIX + addressId);
        if (json == null) {
            throw expired();
        }

        try {
            return objectMapper.readValue(json, AnalysisTargetDTO.class);
        } catch (JsonProcessingException e) {
            redisTemplate.delete(KEY_PREFIX + addressId);
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "주소 정보를 읽지 못했습니다."
            );
        }
    }

    private BusinessException expired() {
        return new BusinessException(
                ErrorCode.RESOURCE_CONFLICT,
                "주소 정보가 만료되었습니다. 다시 검색해주세요."
        );
    }
}
