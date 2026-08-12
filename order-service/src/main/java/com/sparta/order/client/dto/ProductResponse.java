package com.sparta.order.client.dto;

import java.util.UUID;

public record ProductResponse(
        UUID productId,
        UUID hubId,
        String name,
        Long price,
        Long stockQuantity
) {
}
