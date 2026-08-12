package com.secondzip.backend.security.jwt;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

@Component
public class JwtTokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public String resolveAccessToken(HttpServletRequest request) {
        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorizationHeader)) {
            throw new IllegalArgumentException(
                    "Authorization 헤더가 없습니다."
            );
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException(
                    "Bearer 형식의 Access Token이 필요합니다."
            );
        }

        String accessToken =
                authorizationHeader.substring(BEARER_PREFIX.length());

        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException(
                    "Access Token이 없습니다."
            );
        }

        return accessToken;
    }
}