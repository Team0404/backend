package com.sparta.delivery.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * hub-service {@code HubRoutePathResponse} 의 delivery-service 측 대응 DTO.
 * 출발 허브 → 도착 허브까지의 전체 경로(구간 목록)를 표현한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubRoutePathResponseDto {
    private UUID departureHubId;
    private String departureHubName;
    private UUID arrivalHubId;
    private String arrivalHubName;
    private List<HubRouteSegmentResponseDto> routes;
    private Integer totalDurationMinutes;
    private BigDecimal totalDistanceKm;
}
