package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.Delivery;
import com.sparta.delivery.domain.entity.DeliveryStatusEnum;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    boolean existsByOrderId(@NotNull UUID orderId);

    Optional<Delivery> findByDeliveryIdAndDeletedAtIsNull(UUID deliveryId);

    /** 주문 취소 보상 트랜잭션용. */
    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * D3 검색. scopeHubId/scopeManagerId/scopeRouteDeliveryIds는 role별 조회 범위 제한용(둘 다 null이면 MASTER처럼 전체 조회).
     */
    @Query("""
            SELECT d FROM Delivery d
            WHERE d.deletedAt IS NULL
              AND (:status IS NULL OR d.status = :status)
              AND (:destHubId IS NULL OR d.destHubId = :destHubId)
              AND (:recipientName IS NULL OR d.recipientName LIKE CONCAT('%', :recipientName, '%'))
              AND (:scopeHubId IS NULL OR d.originHubId = :scopeHubId OR d.destHubId = :scopeHubId)
              AND (:scopeManagerId IS NULL OR d.companyDeliveryManagerId = :scopeManagerId
                   OR (:scopeRouteDeliveryIds IS NOT EMPTY AND d.deliveryId IN :scopeRouteDeliveryIds))
            """)
    Page<Delivery> search(
            @Param("status") DeliveryStatusEnum status,
            @Param("destHubId") UUID destHubId,
            @Param("recipientName") String recipientName,
            @Param("scopeHubId") UUID scopeHubId,
            @Param("scopeManagerId") UUID scopeManagerId,
            @Param("scopeRouteDeliveryIds") List<UUID> scopeRouteDeliveryIds,
            Pageable pageable
    );
}
