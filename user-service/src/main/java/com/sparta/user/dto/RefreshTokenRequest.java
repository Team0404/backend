package com.sparta.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Refresh Token 요청")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
