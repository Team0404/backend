package com.sparta.order.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_orders")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", length = 50, unique = true)
    private String orderNumber;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "request_note", length = 500)
    private String requestNote;

    @Column(name = "delivery_deadline")
    private LocalDateTime deliveryDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void setDeliveryId(UUID deliveryId) {
        this.deliveryId = deliveryId;
    }

    public void updateDetails(String requestNote, LocalDateTime deliveryDeadline) {
        if (requestNote != null) {
            this.requestNote = requestNote;
        }
        if (deliveryDeadline != null) {
            this.deliveryDeadline = deliveryDeadline;
        }
    }

    public void addOrderItem(OrderItem item) {
        item.assignOrder(this);
        this.orderItems.add(item);
    }
}
