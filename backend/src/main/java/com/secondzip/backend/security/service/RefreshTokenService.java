package com.secondzip.backend.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(
            Long accountId,
            String refreshToken,
            long expirationMillis
    ) {
        redisTemplate.opsForValue().set(
                PREFIX + accountId,
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    public String find(Long accountId) {
        return redisTemplate.opsForValue().get(
                PREFIX + accountId
        );
    }

    public boolean matches(
            Long accountId,
            String refreshToken
    ) {
        String savedToken = find(accountId);

        return savedToken != null
                && savedToken.equals(refreshToken);
    }

    public void delete(Long accountId) {
        redisTemplate.delete(PREFIX + accountId);
    }
}