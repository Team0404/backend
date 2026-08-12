package com.sparta.order.repository;

import com.sparta.order.entity.Order;
import com.sparta.order.repository.query.OrderSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepositoryCustom {

    Page<Order> searchOrders(
            OrderSearchCriteria criteria,
            Pageable pageable
    );

    Optional<Order> findDetailById(OrderSearchCriteria criteria, java.util.UUID orderId);
}
