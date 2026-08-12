package com.secondzip.backend.security.jwt;

import com.secondzip.backend.account.domain.AccountVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(String secret, long accessExpiration, long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(AccountVO account) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpiration);

        //Header.Payload.Signature
        return Jwts.builder()
                .setSubject(String.valueOf(account.getAccountId()))
                .claim("type", "ACCESS")
                .claim("email", account.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(AccountVO account) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(account.getAccountId()))
                .claim("type", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    //전달받은 JWT를 검증한 뒤 Payload 안의 Claims 추출
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token) //JWT 형식, 서명 일치, 만료 시간 초과 검증
                .getBody();
    }

    public long getRemainingExpirationMillis(String token) {
        Date expiration = parseClaims(token).getExpiration();

        return Math.max(expiration.getTime() - System.currentTimeMillis(), 0L);
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}