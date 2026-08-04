package com.sparta.order.repository.query;

import com.sparta.common.entity.UserRole;
import com.sparta.order.entity.OrderStatus;

import java.util.UUID;

public record OrderSearchCriteria(
        UUID companyId,
        OrderStatus status,
        UUID requestUserId,
        UUID requestHubId,
        UserRole userRole
) {
}
