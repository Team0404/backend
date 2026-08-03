package com.sparta.user.dto;

import com.sparta.common.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final Long userId;
    private final UserRole role;
}
