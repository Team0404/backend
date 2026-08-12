package com.sparta.common.dto;

import com.sparta.common.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 서비스 간 통신(FeignClient)에서 공유하는 사용자 정보 응답.
 * user-service가 제공하고, 다른 서비스가 소비한다. (비밀번호 등 민감정보 제외)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private UUID userId;
    private String username;
    private String nickname;
    private String slackId;
    private UserRole role;
    private UUID hubId;
    private UUID companyId;
}
