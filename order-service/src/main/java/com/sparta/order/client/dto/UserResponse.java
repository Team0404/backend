package com.sparta.order.client.dto;

import com.sparta.common.entity.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String nickname,
        String slackId,
        UserRole role,
        UUID hubId,
        UUID companyId
) {
}
