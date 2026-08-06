package com.sparta.user.dto;

import com.sparta.common.entity.UserRole;
import com.sparta.user.entity.ApprovalStatus;
import com.sparta.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserManagementResponse {

    private UUID userId;
    private String username;
    private String nickname;
    private String slackId;
    private UserRole role;
    private ApprovalStatus approvalStatus;
    private String rejectionReason;
    private UUID hubId;
    private UUID companyId;
    private LocalDateTime createdAt;

    public static UserManagementResponse from(User user) {
        return UserManagementResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .approvalStatus(user.getApprovalStatus())
                .rejectionReason(user.getRejectionReason())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
