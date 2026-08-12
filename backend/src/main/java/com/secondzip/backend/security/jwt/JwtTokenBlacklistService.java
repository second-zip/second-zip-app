package com.secondzip.backend.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class JwtTokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void add(String accessToken, long remainingMillis) {
        if (remainingMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                PREFIX + accessToken,
                "logout",
                Duration.ofMillis(remainingMillis)
        );
    }

    public boolean contains(String accessToken) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX + accessToken)
        );
    }
}