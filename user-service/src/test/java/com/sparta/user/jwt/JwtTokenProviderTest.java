package com.sparta.user.jwt;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

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
        String accessToken = provider.createAccessToken(userId, "tester", UserRole.MASTER);

        assertThatThrownBy(() -> provider.getUserIdFromRefreshToken(accessToken))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
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
}
