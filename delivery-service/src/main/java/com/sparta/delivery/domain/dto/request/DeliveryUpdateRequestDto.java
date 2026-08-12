package com.sparta.delivery.domain.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class DeliveryUpdateRequestDto {
    private String status;
    private String deliveryAddress;
    private String recipientName;
    private String recipientSlackId;
    private UUID companyDeliveryManagerId;
}
