package com.sparta.delivery.client;

import com.sparta.common.feign.InternalCallInterceptor;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "order-service", contextId = "orderrClient", path = "/api/v1/orders", configuration = InternalCallInterceptor.class)
public interface OrderClient {

//    @GetMapping("/me")
//    public ApiResponse<OrderResponse> getOrder();
}
