package com.sparta.order.dto.response;

import com.sparta.order.entity.OrderItem;

import java.util.UUID;

public record OrderItemResponse(

        UUID id,
        UUID productId,
        String productName,
        Long unitPrice,
        Integer quantity
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity()
        );
    }
}
