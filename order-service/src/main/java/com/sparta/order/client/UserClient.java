package com.sparta.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserClient {

    @GetMapping("/api/v1/internal/users/{userId}")
    com.sparta.common.response.ApiResponse<com.sparta.order.client.dto.UserResponse> getUser(@PathVariable("userId") UUID userId);
}
