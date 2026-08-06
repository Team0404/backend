package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryRouteStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryRouteUpdateResponseDto {
    private UUID deliveryRouteId;
    private DeliveryRouteStatusEnum status;
}
