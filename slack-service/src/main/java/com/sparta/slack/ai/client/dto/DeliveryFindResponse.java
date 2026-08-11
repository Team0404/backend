package com.sparta.slack.ai.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryFindResponse {
    private UUID deliveryId;
    private UUID orderId;
    private UUID originHubId;
    private UUID destHubId;
    private String deliveryAddress;
    private String recipientName;
    private String recipientSlackId;
    private UUID companyDeliveryManagerId;
}