package com.sparta.delivery.client;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.feign.InternalCallInterceptor;
import com.sparta.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", contextId = "userClient", path = "/api/v1/", configuration = InternalCallInterceptor.class)
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    public ApiResponse<UserInfoResponse> getUser(@PathVariable UUID userId);
}
