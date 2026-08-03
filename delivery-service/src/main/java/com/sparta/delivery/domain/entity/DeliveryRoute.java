package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeliveryRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID deliveryRouteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    public Delivery delivery;

    @Column(name = "sequence", nullable = false)
    public Integer sequence;

    @Column(name = "origin_hub_id", nullable = false)
    public UUID originHubId;

    @Column(name = "dest_hub_id", nullable = false)
    public UUID destHub_id;

    // 테이블 명세대로 한 것인데, 거리를 굳이 BD를 써야하나?
    @Column(name = "expected_distance_km")
    public BigDecimal expected_distance_km;
    @Column(name = "expected_duration_min")
    public Integer expected_duration_min;

    // 테이블 명세대로 한 것인데, 거리를 굳이 BD를 써야하나?
    @Column(name = "actual_distance_km")
    public BigDecimal actual_distance_km;
    @Column(name = "actual_duration_min")
    public Integer actual_duration_min;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    public DeliveryRouteStatusEnum status = DeliveryRouteStatusEnum.HUB_MOVE_WAIT;

    @Column(name = "hub_delivery_manager_id")
    public Long hub_delivery_manager_id;
}
