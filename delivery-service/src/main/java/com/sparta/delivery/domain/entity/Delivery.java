package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

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
        if(!status.isBlank()){
            try {
                this.status = DeliveryStatusEnum.valueOf(status); // 대소문자 구분 방지
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 Status 입니다.");
            }
        }
        if(!deliveryAddress.isBlank()){
            this.deliveryAddress = deliveryAddress;
        }
        if(!recipientName.isBlank()){
            this.recipientName = recipientName;
        }
        if(!recipientSlackId.isBlank()){
            this.recipientSlackId = recipientSlackId;
        }
        if(companyDeliveryManagerId != null){
            this.companyDeliveryManagerId = companyDeliveryManagerId;
        }
    }
}
