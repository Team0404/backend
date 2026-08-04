package com.sparta.order.dto.request;

import com.sparta.order.entity.OrderStatus;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public record OrderSearchRequest(

        UUID companyId,

        OrderStatus status,

        // 정렬 기준: createdAt (기본값), updatedAt
        String sortBy,

        // 정렬 방향: DESC (기본값), ASC
        Sort.Direction direction
) {
}
