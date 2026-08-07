package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_deliveries")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
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

    public void update(
            String status,
            String deliveryAddress,
            String recipientName,
            String recipientSlackId,
            UUID companyDeliveryManagerId
    ){
        if(status != null && !status.isBlank()){
            this.status = DeliveryStatusEnum.fromString(status);
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
