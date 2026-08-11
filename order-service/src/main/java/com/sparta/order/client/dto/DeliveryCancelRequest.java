package com.sparta.order.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryCancelRequest(
        @NotNull UUID orderId,
        @NotBlank String reason
) {
}
