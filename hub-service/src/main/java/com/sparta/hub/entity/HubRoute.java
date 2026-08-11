package com.sparta.hub.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_hub_routes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HubRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID hubRouteId;

    // 출발 허브
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_hub_id", nullable = false)
    private Hub departureHub;

    // 도착 허브
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arrival_hub_id", nullable = false)
    private Hub arrivalHub;

    // 예상 소요 시간
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // 이동 거리
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    public HubRoute(Hub departureHub, Hub arrivalHub, Integer durationMinutes,
                    BigDecimal distanceKm){
        this.departureHub = departureHub;
        this.arrivalHub = arrivalHub;
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;

    }

    public void updateRoute(Integer durationMinutes, BigDecimal distanceKm){
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
    }


}
