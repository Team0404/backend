package com.sparta.user.dto;

import com.sparta.common.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SignupRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-z0-9]{4,10}$",
            message = "아이디는 4~10자의 영문 소문자와 숫자만 사용할 수 있습니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,15}$",
            message = "비밀번호는 8~15자이며 영문, 숫자, 특수문자를 각각 포함해야 합니다.")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @NotBlank(message = "Slack ID는 필수입니다.")
    private String slackId;

    @NotNull(message = "역할은 필수입니다.")
    private UserRole role;

    // 선택값: 역할에 따라 소속 허브/업체 지정
    private UUID hubId;

    private UUID companyId;
}
