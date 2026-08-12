package com.sparta.user.dto;

import com.sparta.user.entity.ApprovalStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "회원가입 응답")
public class SignupResponse {

    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private final UUID userId;

    @Schema(description = "승인 상태", example = "PENDING")
    private final ApprovalStatus approvalStatus;
}
