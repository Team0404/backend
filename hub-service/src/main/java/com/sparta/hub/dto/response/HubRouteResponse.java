package com.sparta.hub.dto.response;

import com.sparta.hub.entity.HubRoute;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class HubRouteResponse {
    // 허브 라우트 ID
    private UUID routeId;

    // 출발 허브 ID, 출발 허브 이름
    private UUID departureHubId;
    private String departureHubName;

    // 도착 허브 ID, 도착 허브 이름
    private UUID arrivalHubId;
    private String arrivalHubName;

    // 소요 시간 및 거리
    private Integer durationMinutes;
    private BigDecimal distanceKm;

    public HubRouteResponse(HubRoute route) {
        this.routeId = route.getHubRouteId();

        this.departureHubId = route.getDepartureHub().getHubId();
        this.departureHubName = route.getDepartureHub().getName();

        this.arrivalHubId = route.getArrivalHub().getHubId();
        this.arrivalHubName = route.getArrivalHub().getName();

        this.durationMinutes = route.getDurationMinutes();
        this.distanceKm = route.getDistanceKm();
    }
}
