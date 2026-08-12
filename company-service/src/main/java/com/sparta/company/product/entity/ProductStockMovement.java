package com.sparta.company.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * decrease-stock/restore-stock 호출을 referenceId(주로 주문 ID) 기준으로 기록해서
 * 같은 요청이 재시도 등으로 중복 호출되어도 재고가 두 번 반영되지 않도록 막는다.
 *
 * referenceId가 없는 호출(Order 쪽에서 아직 안 넘겨주는 경우)은 이 기록을 남기지 않고
 * 예전처럼 매번 그대로 반영한다 - 하위 호환을 위한 선택적 방어 장치.
 */
@Getter
@Entity
@Table(name = "p_product_stock_movement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStockMovement {

    public enum MovementType {
        DECREASE, RESTORE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    private UUID productId;

    // 재처리/재시도 방지 키. 보통 Order 서비스의 주문(or 주문항목) ID.
    @Column(name = "reference_id", nullable = false, length = 100)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private ProductStockMovement(UUID productId, String referenceId, MovementType movementType, Integer quantity) {
        this.productId = productId;
        this.referenceId = referenceId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
    }
}
