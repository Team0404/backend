package com.sparta.delivery.domain.entity;


import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "p_deliveries")
@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Delivery extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID deliveryId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DeliveryStatusEnum status = DeliveryStatusEnum.HUB_WAIT;

    @Column(name = "origin_hub_id", nullable = false)
    private UUID originHubId;

    @Column(name = "dest_hub_id", nullable = false)
    private UUID destHubId;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_slack_id")
    private String recipientSlackId;

    // 업체 -> 허브 / 허브 -> 업체만 담당하는 UserId
    @Column(name = "company_delivery_manager_id")
    private UUID companyDeliveryManagerId;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public void cancel(String cancelReason){
        status.validateTransit(DeliveryStatusEnum.CANCELLED);
        this.status = DeliveryStatusEnum.CANCELLED;
        this.cancelReason = cancelReason;
        this.cancelledAt = LocalDateTime.now();
        // 아직 수행되지 않은 업체 배송이므로 담당자 배정을 해제한다.
        this.companyDeliveryManagerId = null;
    }

    public void update(
            String status,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId,
            UUID companyDeliveryManagerId
    ){
        if(status != null && !status.isBlank()){
            DeliveryStatusEnum next = DeliveryStatusEnum.fromString(status);
            if(next != this.status){
                this.status.validateTransit(next);
                this.status = next;
            }
        }
        if(deliveryAddress != null && !deliveryAddress.isBlank()){
            this.deliveryAddress = deliveryAddress;
        }
        if(recipientName != null && !recipientName.isBlank()){
            this.recipientName = recipientName;
        }
        if(recipientSlackId != null && !recipientSlackId.isBlank()){
            this.recipientSlackId = recipientSlackId;
        }
        if(companyDeliveryManagerId != null){
            this.companyDeliveryManagerId = companyDeliveryManagerId;
        }
    }
}
