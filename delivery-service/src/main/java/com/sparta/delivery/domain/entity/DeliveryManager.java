package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 배송 담당자 (p_delivery_managers).
 *
 * PK(user_id)는 사용자 서비스의 user_id 와 동일한 값(논리 참조)이므로 자동 생성하지 않는다.
 * type=COMPANY 는 hub_id 필수, type=HUB 는 hub_id NULL(전체 풀).
 * sequence 는 라운드로빈 배정 순번으로, 신규 등록 시 그룹의 마지막 순번으로 부여된다.
 */
@Entity
@Table(name = "p_delivery_managers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeliveryManager extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    /**
     * 배송 담당자 타입.
     *  - HUB     : 허브 간 이동 담당 (전체 풀, 소속 허브 없음 → hub_id NULL)
     *  - COMPANY : 최종 허브 → 수령 업체 이동 담당 (허브별 풀 → hub_id 필수)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DeliveryManagerType type;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    // 허브 ID 혹은 업체 ID 혹은 NULL
    @Column(name = "last_assigned_at")
    private Integer lastAssignedAt;
}
