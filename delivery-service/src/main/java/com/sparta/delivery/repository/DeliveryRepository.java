package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.Delivery;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID>, DeliveryRepositoryCustom {
    boolean existsByOrderId(@NotNull UUID orderId);

    Optional<Delivery> findByDeliveryIdAndDeletedAtIsNull(UUID deliveryId);

    /** 주문 취소 보상 트랜잭션용. */
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}
