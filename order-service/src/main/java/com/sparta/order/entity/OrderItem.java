package com.sparta.order.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_order_items")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    private static final String DECREASE_SUFFIX = ":DECREASE";
    private static final String RESTORE_SUFFIX = ":RESTORE";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "stock_operation_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID stockOperationId;

    public void assignOrder(Order order) {
        this.order = order;
    }

    public String decreaseStockReferenceId() {
        return stockOperationId + DECREASE_SUFFIX;
    }

    public String restoreStockReferenceId() {
        return stockOperationId + RESTORE_SUFFIX;
    }
}
