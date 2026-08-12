package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.Delivery;
import com.sparta.delivery.domain.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {
    List<DeliveryRoute> findAllByDeliveryOrderBySequenceAsc(Delivery delivery);

    List<UUID> findHubDeliveryManagerIdByDelivery(Delivery delivery);

    @Query("SELECT r.delivery.deliveryId FROM DeliveryRoute r WHERE r.hubDeliveryManagerId = :hubDeliveryManagerId")
    List<UUID> findDeliveryIdByHubDeliveryManagerId(@Param("hubDeliveryManagerId") UUID hubDeliveryManagerId);
}
