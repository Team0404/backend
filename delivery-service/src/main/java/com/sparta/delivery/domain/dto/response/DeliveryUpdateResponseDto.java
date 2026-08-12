package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryUpdateResponseDto {
    private UUID deliveryId;
    private DeliveryStatusEnum status;
}
