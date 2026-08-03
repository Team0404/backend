package com.sparta.delivery.domain.dto.request;

import lombok.Builder;

@Builder
public class DeliveryUpdateRequestDto {
    private String status;
    private String deliveryAddress;
    private String recipientName;
    private String recipientSlackId;
    private Long companyDeliveryManagerId;
}
