package com.sparta.user.controller;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.response.ApiResponse;
import com.sparta.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "User (Internal)", description = "서비스 간 통신 전용 사용자 API (게이트웨이 라우팅 없음)")
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 정보 조회 (내부 서비스 통신용, FeignClient)")
    @GetMapping("/{userId}")
    public ApiResponse<UserInfoResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.success(userService.getUserInfo(userId));
    }
}
