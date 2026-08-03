package com.sparta.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "로그인 요청")
public class LoginRequest {

    @Schema(description = "아이디", example = "test123")
    @NotBlank(message = "아이디는 필수입니다.")
    private String username;

    @Schema(description = "비밀번호", example = "Test123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
