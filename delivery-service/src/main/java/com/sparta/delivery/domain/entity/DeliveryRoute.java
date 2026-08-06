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
@Table(name = "p_delivery_routes")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeliveryRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    public Delivery delivery;

    @Column(name = "sequence", nullable = false)
    public Integer sequence;

    @Column(name = "origin_hub_id", nullable = false)
    public UUID originHubId;

    @Column(name = "dest_hub_id", nullable = false)
    public UUID destHubId;

    @Column(name = "expected_distance_km")
    public BigDecimal expectedDistanceKm;
    @Column(name = "expected_duration_min")
    public Integer expectedDurationMin;

    @Column(name = "actual_distance_km")
    public BigDecimal actualDistanceKm;
    @Column(name = "actual_duration_min")
    public Integer actualDurationMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    public DeliveryRouteStatusEnum status = DeliveryRouteStatusEnum.HUB_MOVE_WAIT;

    @Column(name = "hub_delivery_manager_id")
    public UUID hubDeliveryManagerId;
}
