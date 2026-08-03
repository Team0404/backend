package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryStatusEnum;
import lombok.Builder;

import java.util.UUID;

@Builder
public class DeliveryUpdateResponseDto {
    private UUID deliveryId;
    private DeliveryStatusEnum status;
}
