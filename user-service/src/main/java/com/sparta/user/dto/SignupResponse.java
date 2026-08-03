package com.sparta.user.dto;

import com.sparta.user.entity.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

    private final Long userId;
    private final ApprovalStatus approvalStatus;
}
