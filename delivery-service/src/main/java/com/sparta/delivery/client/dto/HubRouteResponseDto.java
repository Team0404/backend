package com.sparta.delivery.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Getter
public class HubRouteResponseDto {
    private UUID id;
    private UUID departure_hub_id;
    private UUID arrival_hub_id;
    private Integer duration_minutes;
    private BigDecimal distance_km;
}
