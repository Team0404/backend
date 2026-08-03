package com.sparta.delivery.domain.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public class DeliveryRouteSearchResponseDto {
    private UUID deliveryRouteId;
    private Integer sequence;
    private UUID originHubId;
    private UUID destHubId;
    private BigDecimal expectedDistanceKm;
    private Integer expectedDurationMin;
    private BigDecimal actualDistanceKm;
    private Integer actualDurationMin;
    private String status;
    private Long hubDeliveryManagerId;
}
