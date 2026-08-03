package com.sparta.order.client;

import com.sparta.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "delivery-service", contextId = "deliveryClient", path = "/api/v1/deliveries")
public interface DeliveryClient {

    @PostMapping
    ApiResponse<Map<String, Object>> createDelivery(@RequestBody Map<String, Object> request);

    @DeleteMapping("/{id}")
    ApiResponse<Void> cancelDelivery(@PathVariable("id") UUID deliveryId);
}
