package com.sparta.delivery.domain.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryCreateResponseDto {
    private UUID deliveryId;
    private String status;
    private Integer routeCount;
}
