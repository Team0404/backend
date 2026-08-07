package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliverySummaryResponseDto {
    private UUID deliveryId;
    private UUID orderId;
    private DeliveryStatusEnum status;
    private UUID destHubId;
    private String recipientName;
}
