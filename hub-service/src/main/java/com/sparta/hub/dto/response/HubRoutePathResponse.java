package com.sparta.hub.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class HubRoutePathResponse {

    // 출발 허브, 허브 이름
    private UUID departureHubId;
    private String departureHubName;

    // 도착 허브, 허브 이름
    private UUID arrivalHubId;
    private String arrivalHubName;

    // 이동 경로 리스트
    List<HubRouteResponse> routes;

    // 총 예상 소요 시간
    private Integer totalDurationMinutes;

    // 총 이동 거리
    private BigDecimal totalDistanceKm;

    public HubRoutePathResponse (UUID departureHubId, String departureHubName, UUID arrivalHubId,
                                 String arrivalHubName,
                                 List<HubRouteResponse> routes,
                                 Integer totalDurationMinutes,
                                 BigDecimal totalDistanceKm) {

        this.departureHubId = departureHubId;
        this.departureHubName = departureHubName;
        this.arrivalHubId = arrivalHubId;
        this.arrivalHubName = arrivalHubName;
        this.routes = routes;
        this.totalDurationMinutes = totalDurationMinutes;
        this.totalDistanceKm = totalDistanceKm;
    }

}
