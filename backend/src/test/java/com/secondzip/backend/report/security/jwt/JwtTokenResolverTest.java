package com.secondzip.backend.report.security.jwt;

import com.secondzip.backend.security.jwt.JwtTokenResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenResolverTest {

    private JwtTokenResolver jwtTokenResolver;

    @BeforeEach
    void setUp() {
        jwtTokenResolver = new JwtTokenResolver();
    }

    @Test
    @DisplayName("Bearer 형식의 Authorization 헤더에서 Access Token을 추출한다")
    void resolveAccessToken_validBearerToken_returnsToken() {
        // given == 사전 데이터
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");

        // when == 실행
        String token = jwtTokenResolver.resolveAccessToken(request);

        // then == 결과
        assertEquals("access-token", token);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 예외가 발생한다")
    void resolveAccessToken_missingAuthorizationHeader_throwsException() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenResolver.resolveAccessToken(request)
        );

        // then
        assertEquals(
                "Authorization 헤더가 없습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 예외가 발생한다")
    void resolveAccessToken_invalidBearerFormat_throwsException() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "access-token");

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenResolver.resolveAccessToken(request)
        );

        // then
        assertEquals(
                "Bearer 형식의 Access Token이 필요합니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Bearer 뒤에 Access Token이 없으면 예외가 발생한다")
    void resolveAccessToken_emptyAccessToken_throwsException() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenResolver.resolveAccessToken(request)
        );

        // then
        assertEquals(
                "Access Token이 없습니다.",
                exception.getMessage()
        );
    }
}