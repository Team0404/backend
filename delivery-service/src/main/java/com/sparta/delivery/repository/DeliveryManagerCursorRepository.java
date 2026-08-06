package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.DeliveryManagerCursor;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerCursorRepository extends JpaRepository<DeliveryManagerCursor, UUID> {
    Optional<DeliveryManagerCursor> findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType type, UUID hubId);
}
