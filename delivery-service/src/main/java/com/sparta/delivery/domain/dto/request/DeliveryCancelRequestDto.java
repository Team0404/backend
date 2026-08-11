package com.sparta.delivery.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryCancelRequestDto {
    @NotNull
    private UUID orderId;
    @NotNull
    @NotBlank
    private String reason;
}
