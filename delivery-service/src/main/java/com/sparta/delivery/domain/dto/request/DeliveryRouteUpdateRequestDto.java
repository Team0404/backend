package com.sparta.delivery.domain.dto.request;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class DeliveryRouteUpdateRequestDto {
    private String status;
    private BigDecimal actualDistanceKm;
    private Integer actualDurationMin;
    private Long hubDeliveryManagerId;
}
