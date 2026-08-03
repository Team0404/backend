package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryRouteStatusEnum;

import java.util.UUID;

public class DeliveryRouteUpdateResponseDto {
    private UUID deliveryRouteId;
    private DeliveryRouteStatusEnum status;
}
