package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
//import lombok.Value;


import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Delivery extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID deliveryId;

    @Column(name = "order_id", nullable = false, unique = true)
    public UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public DeliveryStatusEnum status = DeliveryStatusEnum.HUB_WAIT;

    @Column(name = "origin_hub_id", nullable = false)
    public UUID originHubId;

    @Column(name = "dest_hub_id", nullable = false)
    public UUID destHubId;

    @Column(name = "delivery_address", nullable = false)
    public String deliveryAddress;

    @Column(name = "recipient_name", nullable = false)
    public String recipientName;

    @Column(name = "recipient_slack_id")
    public String recipientSlackId;

    @Column(name = "company_delivery_manager_id")
    public Long companyDeliveryManagerId;
}
