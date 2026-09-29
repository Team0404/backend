package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.DeliveryManagerCursor;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerCursorRepository extends JpaRepository<DeliveryManagerCursor, UUID> {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    Optional<DeliveryManagerCursor> findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType type, UUID hubId);
}
