package com.sparta.order.dto.response;

import com.sparta.order.entity.OrderItem;

import java.util.UUID;

public record OrderItemResponse(

        UUID id,
        UUID productId,
        Integer quantity
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity()
        );
    }
}
