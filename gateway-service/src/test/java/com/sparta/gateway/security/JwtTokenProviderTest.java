package com.sparta.gateway.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET =
            "local-test-jwt-secret-key-that-is-long-enough-0123456789";

    @Test
    void accessTokenIsAccepted() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET);
        String token = token("ACCESS");

        assertThat(provider.validateAndExtract(token).get("tokenType", String.class))
                .isEqualTo("ACCESS");
    }

    @Test
    void refreshTokenCannotAuthenticateGatewayRequest() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET);
        String token = token("REFRESH");

        assertThatThrownBy(() -> provider.validateAndExtract(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void accessTokenWithoutIdIsRejected() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET);
        String token = token("ACCESS", false);

        assertThatThrownBy(() -> provider.validateAndExtract(token))
                .isInstanceOf(JwtException.class);
    }

    private String token(String tokenType) {
        return token(tokenType, true);
    }

    private String token(String tokenType, boolean includeId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000));
        if (includeId) {
            builder.id(UUID.randomUUID().toString());
        }
        return builder.signWith(key).compact();
    }
}
