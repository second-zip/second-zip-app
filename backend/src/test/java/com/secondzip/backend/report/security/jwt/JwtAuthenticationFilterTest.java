package com.secondzip.backend.report.security.jwt;

import com.secondzip.backend.security.jwt.JwtAuthenticationFilter;
import com.secondzip.backend.security.jwt.JwtTokenBlacklistService;
import com.secondzip.backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtTokenBlacklistService jwtTokenBlacklistService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        jwtTokenProvider,
                        jwtTokenBlacklistService
                );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    @Nested
    @DisplayName("JWT 인증")
    class AuthenticationTest {

        @Test
        @DisplayName("정상 Access Token이면 SecurityContext에 인증 정보를 저장한다")
        void validAccessToken_setsAuthentication() throws Exception {
            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            request.addHeader(
                    "Authorization",
                    "Bearer access-token"
            );

            Claims claims = mock(Claims.class);

            when(jwtTokenBlacklistService.contains("access-token"))
                    .thenReturn(false);

            when(jwtTokenProvider.parseClaims("access-token"))
                    .thenReturn(claims);

            when(claims.get("type", String.class))
                    .thenReturn("ACCESS");

            when(claims.getSubject())
                    .thenReturn("1");

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            Authentication authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();

            assertNotNull(authentication);

            assertEquals(
                    1L,
                    authentication.getPrincipal()
            );

            assertTrue(authentication.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            authority.getAuthority()
                                    .equals("ROLE_USER")
                    ));

            assertSame(
                    request,
                    authentication.getDetails()
            );

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }


        @Test
        @DisplayName("Authorization 헤더가 없으면 인증하지 않고 다음 필터로 진행한다")
        void missingAuthorizationHeader_skipsAuthentication()
                throws Exception {

            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            assertNull(
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            verifyNoInteractions(jwtTokenProvider);
            verifyNoInteractions(jwtTokenBlacklistService);

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }


        @Test
        @DisplayName("Bearer 형식이 아니면 인증하지 않고 다음 필터로 진행한다")
        void invalidAuthorizationFormat_skipsAuthentication()
                throws Exception {

            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            request.addHeader(
                    "Authorization",
                    "access-token"
            );

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            assertNull(
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            verifyNoInteractions(jwtTokenProvider);
            verifyNoInteractions(jwtTokenBlacklistService);

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }


        @Test
        @DisplayName("블랙리스트에 등록된 Access Token이면 인증하지 않는다")
        void blacklistedToken_doesNotAuthenticate()
                throws Exception {

            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            request.addHeader(
                    "Authorization",
                    "Bearer access-token"
            );

            when(jwtTokenBlacklistService.contains("access-token"))
                    .thenReturn(true);

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            assertNull(
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            verify(jwtTokenBlacklistService)
                    .contains("access-token");

            verify(jwtTokenProvider, never())
                    .parseClaims(anyString());

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }


        @Test
        @DisplayName("Refresh Token이면 일반 API 인증에 사용하지 않는다")
        void refreshToken_doesNotAuthenticate()
                throws Exception {

            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            request.addHeader(
                    "Authorization",
                    "Bearer refresh-token"
            );

            Claims claims = mock(Claims.class);

            when(jwtTokenBlacklistService.contains("refresh-token"))
                    .thenReturn(false);

            when(jwtTokenProvider.parseClaims("refresh-token"))
                    .thenReturn(claims);

            when(claims.get("type", String.class))
                    .thenReturn("REFRESH");

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            assertNull(
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }


        @Test
        @DisplayName("잘못된 JWT이면 인증 정보를 저장하지 않는다")
        void invalidJwt_doesNotAuthenticate()
                throws Exception {

            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            request.addHeader(
                    "Authorization",
                    "Bearer invalid-token"
            );

            when(jwtTokenBlacklistService.contains("invalid-token"))
                    .thenReturn(false);

            when(jwtTokenProvider.parseClaims("invalid-token"))
                    .thenThrow(
                            new IllegalArgumentException(
                                    "invalid token"
                            )
                    );

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            assertNull(
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }


        @Test
        @DisplayName("이미 인증 정보가 존재하면 JWT 인증을 다시 수행하지 않는다")
        void existingAuthentication_skipsJwtAuthentication()
                throws Exception {

            // given
            MockHttpServletRequest request =
                    new MockHttpServletRequest();

            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            request.addHeader(
                    "Authorization",
                    "Bearer access-token"
            );

            Authentication existingAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            99L,
                            null,
                            null
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(existingAuthentication);

            // when
            jwtAuthenticationFilter.doFilter(
                    request,
                    response,
                    filterChain
            );

            // then
            assertSame(
                    existingAuthentication,
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

            verifyNoInteractions(jwtTokenProvider);
            verifyNoInteractions(jwtTokenBlacklistService);

            verify(filterChain).doFilter(
                    request,
                    response
            );
        }
    }
}