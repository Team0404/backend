package com.sparta.order.dto.request;

import com.sparta.order.entity.OrderStatus;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public record OrderSearchRequest(

        UUID companyId,

        OrderStatus status,

        // 정렬 기준: createdAt (기본값), updatedAt
        @Pattern(
                regexp = "^(createdAt|updatedAt)?$",
                message = "정렬 기준은 createdAt 또는 updatedAt만 사용할 수 있습니다."
        )
        String sortBy,

        // 정렬 방향: DESC (기본값), ASC
        Sort.Direction direction
) {

}
