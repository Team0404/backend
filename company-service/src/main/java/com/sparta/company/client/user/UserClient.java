package com.sparta.company.client.user;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * User 서비스 실제 컨트롤러(UserController) 기준:
 * - 게이트웨이 라우팅 없는 내부 통신 전용: /api/v1/internal/users/{userId}
 * - 응답 DTO(UserInfoResponse)가 common 모듈에 있어 별도 로컬 DTO 없이 바로 사용
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/internal/users/{userId}")
    ApiResponse<UserInfoResponse> getUser(@PathVariable("userId") UUID userId);
}
