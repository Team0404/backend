package com.sparta.order.dto.response;

import com.sparta.order.entity.Order;
import com.sparta.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(

        UUID id,
        String orderNumber,
        UUID companyId,
        UUID hubId,
        UUID deliveryId,
        String requestNote,
        LocalDateTime deliveryDeadline,
        OrderStatus status,
        List<OrderItemResponse> orderItems,
        LocalDateTime createdAt,
        UUID createdBy,
        LocalDateTime updatedAt,
        UUID updatedBy
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCompanyId(),
                order.getHubId(),
                order.getDeliveryId(),
                order.getRequestNote(),
                order.getDeliveryDeadline(),
                order.getStatus(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getCreatedAt(),
                order.getCreatedBy(),
                order.getUpdatedAt(),
                order.getUpdatedBy()
        );
    }
}
