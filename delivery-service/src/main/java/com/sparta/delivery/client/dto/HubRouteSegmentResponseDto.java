package com.sparta.delivery.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * hub-service {@code HubRouteResponse} 의 delivery-service 측 대응 DTO.
 * 경로상 한 구간(허브 → 허브)을 표현한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubRouteSegmentResponseDto {
    private UUID routeId;
    private UUID departureHubId;
    private String departureHubName;
    private UUID arrivalHubId;
    private String arrivalHubName;
    private Integer durationMinutes;
    private BigDecimal distanceKm;
}
