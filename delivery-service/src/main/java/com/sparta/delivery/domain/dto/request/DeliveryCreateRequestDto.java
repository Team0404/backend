package com.sparta.delivery.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryCreateRequestDto {
    @NotNull
    private UUID orderId;
    @NotNull
    private UUID originHubId;
    @NotNull
    private UUID destHubId;
    @NotNull
    @NotBlank
    private String deliveryAddress;
    @NotNull
    @NotBlank
    private String recipientName;

    private String recipientSlackId;
}
