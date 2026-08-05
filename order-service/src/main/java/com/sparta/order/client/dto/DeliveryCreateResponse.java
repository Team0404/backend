package com.sparta.order.client.dto;

import java.util.UUID;

public record DeliveryCreateResponse(
        UUID deliveryId,
        String status,
        Integer routeCount
) {
}
