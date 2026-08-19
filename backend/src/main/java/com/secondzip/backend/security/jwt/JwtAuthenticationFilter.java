package com.secondzip.backend.security.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = resolveToken(request);

        // 토큰이 존재하고, 현재 SecurityContext에 인증 정보가 없을 때만 인증 처리
        if (accessToken != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 1. 블랙리스트 확인
                if (jwtTokenBlacklistService.contains(accessToken)) {
                    throw new IllegalArgumentException(
                            "로그아웃된 Access Token입니다."
                    );
                }

                // 2. 서명·만료 검증 및 Claims 추출
                Claims claims = jwtTokenProvider.parseClaims(accessToken);

                // 3. Access Token인지 확인
                String tokenType = claims.get("type", String.class);

                // Refresh Token이 일반 API 인증에 사용되는 것을 방지
                if (!"ACCESS".equals(tokenType)) {
                    throw new IllegalArgumentException(
                            "Access Token이 아닙니다."
                    );
                }

                // 4. 회원 ID 추출
                Long accountId = Long.valueOf(claims.getSubject());

                // 현재 회원 권한이 하나뿐이므로 고정 권한 사용

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        accountId,
                        null,
                        Collections.singletonList(authority));

                // 요청 정보도 Authentication에 저장
                authentication.setDetails(request);

                // 5. 인증 정보 등록
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception e) {
                // 잘못된 토큰이면 인증 정보를 비움
                SecurityContextHolder.clearContext();
                log.warn(
                        "JWT 인증 실패: {}",
                        e.getMessage()
                );
            }
        }

        // 다음 필터 또는 Controller로 요청 전달
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorizationHeader.substring(
                BEARER_PREFIX.length()
        );
    }
}