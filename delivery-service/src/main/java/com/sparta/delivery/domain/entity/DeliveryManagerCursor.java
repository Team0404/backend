package com.sparta.delivery.domain.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "p_delivery_manager_cursors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cursor_type_hub_id",columnNames = {"type", "hub_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class DeliveryManagerCursor extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // HUB 허브 배송 담당자 / COMPANY 업체 배송 담당자
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DeliveryManagerType type;

    /**
     * PostgreSQL은 UNIQUE(type, hub_id) 제약에서 NULL을 서로 다른 값으로 취급하므로
     * HUB 타입 행을 hub_id = NULL로 여러 개 넣어도 유니크 제약이 안 걸림
     * -> HUB의 전역 그룹은 hub_id를 NULL로 두지 말고 고정된 sentinel UUID(전부 0으로 채운 UUID)를 사용함.
     */
    public static final UUID GLOBAL_HUB_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Column(name = "hub_id", nullable = false)
    @Builder.Default
    private UUID hubId = GLOBAL_HUB_ID;

    // NULL이면 아직 아무 할당이 안된 상태
    @Column(name = "last_assigned_manager_id")
    private UUID lastAssignedManagerId;

    public void updateLastManager(UUID managerId){
        lastAssignedManagerId = managerId;
    }
}
