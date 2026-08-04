package com.sparta.delivery.domain.dto.request;

import lombok.Builder;

import java.util.UUID;

@Builder
public class DeliveryUpdateRequestDto {
    private String status;
    private String deliveryAddress;
    private String recipientName;
    private String recipientSlackId;
    private UUID companyDeliveryManagerId;
}
