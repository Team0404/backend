package com.sparta.user.jwt;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * Access Token과 Refresh Token 발급기.
 * gateway 의 JwtTokenProvider(검증)와 동일한 시크릿/클레임 구조를 사용해야 한다.
 *  - subject : userId
 *  - Access Token claim : "username", "role", "tokenType"
 *  - Refresh Token claim : "tokenType"
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-expiration}") long accessExpiration,
                            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String createAccessToken(UUID userId, String username, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role.name())
                .claim("tokenType", TokenType.ACCESS.name())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("tokenType", TokenType.REFRESH.name())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public UUID getUserIdFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TokenType.REFRESH.name().equals(claims.get("tokenType", String.class))) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효한 Refresh Token이 아닙니다.");
            }
            return UUID.fromString(claims.getSubject());
        } catch (BusinessException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효한 Refresh Token이 아닙니다.");
        }
    }

    public Duration getRefreshTokenTtl() {
        return Duration.ofMillis(refreshExpiration);
    }
}
