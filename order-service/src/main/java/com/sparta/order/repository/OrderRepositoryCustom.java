package com.sparta.order.repository;

import com.sparta.order.entity.Order;
import com.sparta.order.repository.query.OrderSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

    Page<Order> searchOrders(
            OrderSearchCriteria criteria,
            Pageable pageable
    );
}
