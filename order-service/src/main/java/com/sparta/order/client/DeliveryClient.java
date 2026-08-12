package com.sparta.order.client;

import com.sparta.common.feign.InternalCallInterceptor;
import com.sparta.common.response.ApiResponse;
import com.sparta.order.client.dto.DeliveryCancelRequest;
import com.sparta.order.client.dto.DeliveryCreateRequest;
import com.sparta.order.client.dto.DeliveryCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service", contextId = "deliveryClient", path = "/api/v1/deliveries",
        configuration = InternalCallInterceptor.class)
public interface DeliveryClient {

    @PostMapping
    ApiResponse<DeliveryCreateResponse> createDelivery(@RequestBody DeliveryCreateRequest request);

    @PatchMapping("/cancel")
    ApiResponse<Void> cancelDelivery(
            @RequestBody DeliveryCancelRequest request
    );
}
