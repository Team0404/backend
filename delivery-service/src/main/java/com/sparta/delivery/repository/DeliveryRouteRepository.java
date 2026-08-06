package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.Delivery;
import com.sparta.delivery.domain.entity.DeliveryRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {
//    List<DeliveryRoute> findAllByDelivery(Delivery delivery);

    List<UUID> findHubDeliveryManagerIdByDelivery(Delivery delivery);
}
