package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryStatusEnum;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public class DeliveryFindResponseDto {
    private UUID deliveryId;
    private UUID orderId;
    private DeliveryStatusEnum status;
    private UUID originHubId;
    private UUID destHubId;
    private String deliveryAddress;
    private String recipientName;
    private String recipientSlackId;
    private UUID companyDeliveryManagerId;
    private List<DeliveryRouteSearchResponseDto> routes;
}
