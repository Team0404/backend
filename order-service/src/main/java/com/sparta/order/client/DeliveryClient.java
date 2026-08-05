package com.sparta.order.client;

import com.sparta.common.response.ApiResponse;
import com.sparta.order.client.dto.DeliveryCreateRequest;
import com.sparta.order.client.dto.DeliveryCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "delivery-service", contextId = "deliveryClient", path = "/api/v1/deliveries")
public interface DeliveryClient {

    @PostMapping
    ApiResponse<DeliveryCreateResponse> createDelivery(@RequestBody DeliveryCreateRequest request);

    @DeleteMapping("/{id}")
    ApiResponse<Void> cancelDelivery(@PathVariable("id") UUID deliveryId);
}
