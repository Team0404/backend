package com.sparta.delivery.domain.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
public class DeliveryRouteUpdateRequestDto {
    private String status;
    private BigDecimal actualDistanceKm;
    private Integer actualDurationMin;
    private UUID hubDeliveryManagerId;
}
