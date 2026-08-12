package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "p_delivery_routes")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class DeliveryRoute extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
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

    public void cancel() {
        if (!status.canTransit(DeliveryRouteStatusEnum.CANCELLED)) {
            return;
        }
        this.status = DeliveryRouteStatusEnum.CANCELLED;
        this.hubDeliveryManagerId = null;
    }

    public void update(
            String status,
            BigDecimal actualDistanceKm,
            Integer actualDurationMin,
            UUID hubDeliveryManagerId
    ) {
        if (status != null && !status.isBlank()) {
            DeliveryRouteStatusEnum next;
            try {
                next = DeliveryRouteStatusEnum.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 Status 입니다.");
            }
            if (next != this.status) {
                this.status.validateTransit(next);
                this.status = next;
            }
        }
        if (actualDistanceKm != null) {
            this.actualDistanceKm = actualDistanceKm;
        }
        if (actualDurationMin != null) {
            this.actualDurationMin = actualDurationMin;
        }
        if (hubDeliveryManagerId != null) {
            this.hubDeliveryManagerId = hubDeliveryManagerId;
        }
    }
}
