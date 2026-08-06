package com.sparta.order.client.dto;

import java.util.UUID;

public record DeliveryCreateRequest(
        UUID orderId,
        UUID originHubId,
        UUID destHubId,
        String deliveryAddress,
        String recipientName,
        String recipientSlackId
) {
}
