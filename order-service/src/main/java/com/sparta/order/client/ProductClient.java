package com.sparta.order.client;

import com.sparta.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "company-service", contextId = "productClient", path = "/api/v1/products")
public interface ProductClient {

    @PostMapping("/{id}/decrease-stock")
    ApiResponse<Void> decreaseStock(@PathVariable("id") UUID productId, @RequestParam("quantity") Integer quantity);

    @PostMapping("/{id}/restore-stock")
    ApiResponse<Void> restoreStock(@PathVariable("id") UUID productId, @RequestParam("quantity") Integer quantity);
}
