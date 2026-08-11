package com.sparta.user.jwt;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET =
            "local-test-jwt-secret-key-that-is-long-enough-0123456789";

    @Test
    void refreshTokenContainsUserIdAndRefreshType() {
        UUID userId = UUID.randomUUID();
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 60_000);

        String refreshToken = provider.createRefreshToken(userId);

        assertThat(provider.getUserIdFromRefreshToken(refreshToken)).isEqualTo(userId);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        UUID userId = UUID.randomUUID();
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 60_000);
        String accessToken = provider.createAccessToken(
                userId, "tester", UserRole.MASTER, null, null);

        assertThatThrownBy(() -> provider.getUserIdFromRefreshToken(accessToken))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void accessTokenContainsAffiliationClaimsWhenPresent() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 60_000);
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        String accessToken = provider.createAccessToken(
                userId, "tester", UserRole.MASTER, hubId, companyId);
        Claims claims = parseClaims(accessToken);

        assertThat(claims.get("hubId", String.class)).isEqualTo(hubId.toString());
        assertThat(claims.get("companyId", String.class)).isEqualTo(companyId.toString());
    }

    @Test
    void accessTokenOmitsNullAffiliationClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, 60_000);

        String accessToken = provider.createAccessToken(
                UUID.randomUUID(), "tester", UserRole.MASTER, null, null);
        Claims claims = parseClaims(accessToken);

        assertThat(claims).doesNotContainKeys("hubId", "companyId");
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        UUID userId = UUID.randomUUID();
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000, -1);
        String refreshToken = provider.createRefreshToken(userId);

        assertThatThrownBy(() -> provider.getUserIdFromRefreshToken(refreshToken))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
