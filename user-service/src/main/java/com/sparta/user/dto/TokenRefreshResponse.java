package com.sparta.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "토큰 재발급 응답")
public class TokenRefreshResponse {

    @Schema(description = "새 JWT Access Token")
    private final String accessToken;

    @Schema(description = "새 JWT Refresh Token")
    private final String refreshToken;
}
