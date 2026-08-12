package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.Delivery;
import com.sparta.delivery.repository.query.DeliverySearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryRepositoryCustom {

    Page<Delivery> search(DeliverySearchCriteria criteria, Pageable pageable);
}
