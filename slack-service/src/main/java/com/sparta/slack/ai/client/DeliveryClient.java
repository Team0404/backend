package com.sparta.slack.ai.client;

import com.sparta.common.response.ApiResponse;
import com.sparta.slack.ai.client.dto.DeliveryFindResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "delivery-service", contextId = "deliveryClient", path = "/api/v1/deliveries")
public interface DeliveryClient {

    @GetMapping("/{deliveryId}")
    ApiResponse<DeliveryFindResponse> findDelivery(@PathVariable("deliveryId") UUID deliveryId);
}
