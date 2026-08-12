package com.sparta.delivery.client;

import com.sparta.common.feign.InternalCallInterceptor;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.client.dto.HubRoutePathResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * hub-service {@code GET /api/v1/hub-routes/path?dhId=&arId=}.
 * D1에서 origin→dest 구간 경로를 조회해 {@code DeliveryRoute} 를 생성하는 데 쓰인다.
 */
@FeignClient(name = "hub-service", contextId = "hubRouteClient", path = "/api/v1/hub-routes",
        configuration = InternalCallInterceptor.class)
public interface HubRouteClient {

    @GetMapping("/path")
    ApiResponse<HubRoutePathResponseDto> findRoutePath(
            @RequestParam("dhId") UUID departureHubId,
            @RequestParam("arId") UUID arrivalHubId
    );
}
