package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryStatusEnum;
import lombok.Builder;

import java.util.UUID;

@Builder
public class DeliverySummaryResponseDto {
    private UUID deliveryId;
    private UUID orderId;
    private DeliveryStatusEnum status;
    private UUID destHubId;
    private String recipientName;
}
