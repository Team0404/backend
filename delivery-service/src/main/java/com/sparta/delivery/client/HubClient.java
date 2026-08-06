package com.sparta.delivery.client;

import com.sparta.common.feign.InternalCallInterceptor;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.client.dto.HubRouteResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name="hub-service", contextId = "hubClient", path = "/api/v1/hubs", configuration = InternalCallInterceptor.class)
public interface HubClient {
    @GetMapping("/hub-routes/path")
    ApiResponse<List<HubRouteResponseDto>> getRouteOriginToDest();
}
