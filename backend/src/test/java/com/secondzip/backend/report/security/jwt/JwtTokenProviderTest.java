package com.secondzip.backend.report.security.jwt;

import com.secondzip.backend.account.domain.Account;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-test-secret-key-1234567890";

    private static final long ACCESS_EXPIRATION = 60_000L;
    private static final long REFRESH_EXPIRATION = 120_000L;

    private JwtTokenProvider jwtTokenProvider;
    private Account account;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                SECRET,
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION
        );

        account = Account.builder()
                .accountId(1L)
                .email("test@test.com")
                .build();
    }

    @Test
    @DisplayName("Access Token을 생성하면 계정 정보와 ACCESS 타입이 Claims에 저장된다")
    void createAccessToken_validAccount_containsAccessClaims() {
        // given
        // setUp()에서 account 준비

        // when
        String accessToken = jwtTokenProvider.createAccessToken(account);
        Claims claims = jwtTokenProvider.parseClaims(accessToken);

        // then
        assertNotNull(accessToken);
        assertFalse(accessToken.isBlank());

        assertEquals("1", claims.getSubject());
        assertEquals("ACCESS", claims.get("type"));
        assertEquals("test@test.com", claims.get("email"));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Refresh Token을 생성하면 계정 ID와 REFRESH 타입이 Claims에 저장된다")
    void createRefreshToken_validAccount_containsRefreshClaims() {
        // given
        // setUp()에서 account 준비

        // when
        String refreshToken = jwtTokenProvider.createRefreshToken(account);
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);

        // then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isBlank());

        assertEquals("1", claims.getSubject());
        assertEquals("REFRESH", claims.get("type"));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("정상적인 Access Token은 Claims를 추출할 수 있다")
    void parseClaims_validAccessToken_returnsClaims() {
        // given
        String accessToken = jwtTokenProvider.createAccessToken(account);

        // when
        Claims claims = jwtTokenProvider.parseClaims(accessToken);

        // then
        assertEquals("1", claims.getSubject());
        assertEquals("ACCESS", claims.get("type"));
        assertEquals("test@test.com", claims.get("email"));
    }

    @Test
    @DisplayName("다른 Secret Key로 생성한 토큰은 검증에 실패한다")
    void parseClaims_invalidSignature_throwsException() {
        // given
        JwtTokenProvider anotherProvider = new JwtTokenProvider(
                "another-secret-key-another-secret-key-0987654321",
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION
        );

        String invalidToken = anotherProvider.createAccessToken(account);

        // when & then
        assertThrows(
                JwtException.class,
                () -> jwtTokenProvider.parseClaims(invalidToken)
        );
    }

    @Test
    @DisplayName("만료된 Access Token은 검증에 실패한다")
    void parseClaims_expiredAccessToken_throwsException() {
        // given
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                SECRET,
                -1_000L,
                REFRESH_EXPIRATION
        );

        String expiredToken =
                expiredTokenProvider.createAccessToken(account);

        // when & then
        assertThrows(
                ExpiredJwtException.class,
                () -> expiredTokenProvider.parseClaims(expiredToken)
        );
    }

    @Test
    @DisplayName("Access Token의 남은 만료 시간을 반환한다")
    void getRemainingExpirationMillis_validToken_returnsRemainingTime() {
        // given
        String accessToken =
                jwtTokenProvider.createAccessToken(account);

        // when
        long remaining =
                jwtTokenProvider.getRemainingExpirationMillis(accessToken);

        // then
        assertTrue(remaining > 0);
        assertTrue(remaining <= ACCESS_EXPIRATION);
    }

    @Test
    @DisplayName("설정된 Refresh Token 만료 시간을 반환한다")
    void getRefreshExpiration_returnsConfiguredExpiration() {
        // given
        // 생성자에서 REFRESH_EXPIRATION 설정

        // when
        long expiration =
                jwtTokenProvider.getRefreshExpiration();

        // then
        assertEquals(
                REFRESH_EXPIRATION,
                expiration
        );
    }
}