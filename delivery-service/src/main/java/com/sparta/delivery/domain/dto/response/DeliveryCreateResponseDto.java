package com.sparta.delivery.domain.dto.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public class DeliveryCreateResponseDto {
    private UUID deliveryId;
    private String status;
    private Integer routeCount;
}
