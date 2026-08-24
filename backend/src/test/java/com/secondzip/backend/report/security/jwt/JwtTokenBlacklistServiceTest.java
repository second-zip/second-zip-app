package com.secondzip.backend.report.security.jwt;

import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtTokenBlacklistService jwtTokenBlacklistService;

    @Test
    @DisplayName("Access Token을 블랙리스트에 저장한다")
    void add_validToken_savesBlacklist() {
        // given
        String accessToken = "access-token";
        long remainingMillis = 60_000L;

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        // when
        jwtTokenBlacklistService.add(
                accessToken,
                remainingMillis
        );

        // then
        verify(valueOperations).set(
                "blacklist:access-token",
                "logout",
                Duration.ofMillis(remainingMillis)
        );
    }

    @Test
    @DisplayName("남은 만료 시간이 0이면 블랙리스트에 저장하지 않는다")
    void add_zeroRemainingMillis_doesNotSave() {
        // given
        String accessToken = "access-token";
        long remainingMillis = 0L;

        // when
        jwtTokenBlacklistService.add(
                accessToken,
                remainingMillis
        );

        // then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("남은 만료 시간이 음수이면 블랙리스트에 저장하지 않는다")
    void add_negativeRemainingMillis_doesNotSave() {
        // given
        String accessToken = "access-token";
        long remainingMillis = -1_000L;

        // when
        jwtTokenBlacklistService.add(
                accessToken,
                remainingMillis
        );

        // then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("블랙리스트에 Access Token이 존재하면 true를 반환한다")
    void contains_existingToken_returnsTrue() {
        // given
        String accessToken = "access-token";

        when(redisTemplate.hasKey("blacklist:access-token"))
                .thenReturn(true);

        // when
        boolean result =
                jwtTokenBlacklistService.contains(accessToken);

        // then
        assertTrue(result);

        verify(redisTemplate)
                .hasKey("blacklist:access-token");
    }

    @Test
    @DisplayName("블랙리스트에 Access Token이 존재하지 않으면 false를 반환한다")
    void contains_missingToken_returnsFalse() {
        // given
        String accessToken = "access-token";

        when(redisTemplate.hasKey("blacklist:access-token"))
                .thenReturn(false);

        // when
        boolean result =
                jwtTokenBlacklistService.contains(accessToken);

        // then
        assertFalse(result);

        verify(redisTemplate)
                .hasKey("blacklist:access-token");
    }
}