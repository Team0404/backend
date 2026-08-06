package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.Delivery;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    boolean existsByOrderId(@NotNull UUID orderId);

    Optional<Delivery> findByDeliveryIdAndDeletedAtIsNull(UUID deliveryId);
}
