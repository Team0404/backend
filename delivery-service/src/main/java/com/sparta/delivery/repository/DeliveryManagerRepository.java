package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {
    Optional<DeliveryManager> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<DeliveryManager> findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType type, UUID hubId);

    List<DeliveryManager> findAllByUserIdAndDeletedAtIsNull(UUID lastAssignedManagerId);
}
