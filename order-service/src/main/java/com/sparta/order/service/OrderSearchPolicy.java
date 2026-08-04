package com.sparta.order.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.util.PageableUtil;
import com.sparta.order.dto.request.OrderSearchRequest;
import com.sparta.order.repository.query.OrderSearchCriteria;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.UUID;

public final class OrderSearchPolicy {

    private static final String DEFAULT_SORT_PROPERTY = "createdAt";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt", "updatedAt");

    private OrderSearchPolicy() {
    }

    public static OrderSearchCriteria toCriteria(
            OrderSearchRequest request,
            UUID requestUserId,
            UUID requestHubId,
            UserRole userRole
    ) {
        return new OrderSearchCriteria(
                request.companyId(),
                request.status(),
                requestUserId,
                requestHubId,
                userRole
        );
    }

    public static Pageable resolvePageable(OrderSearchRequest request, Pageable pageable) {
        Pageable normalizedPageable = PageableUtil.normalize(pageable);
        if (normalizedPageable.getSort().isSorted()) {
            return PageRequest.of(
                    normalizedPageable.getPageNumber(),
                    normalizedPageable.getPageSize(),
                    normalizeSort(normalizedPageable.getSort())
            );
        }

        return PageRequest.of(
                normalizedPageable.getPageNumber(),
                normalizedPageable.getPageSize(),
                Sort.by(resolveDirection(request.direction()), resolveSortProperty(request.sortBy()))
        );
    }

    private static Sort normalizeSort(Sort sort) {
        return Sort.by(
                sort.stream()
                        .map(order -> new Sort.Order(order.getDirection(), resolveSortProperty(order.getProperty())))
                        .toList()
        );
    }

    private static String resolveSortProperty(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || !ALLOWED_SORT_PROPERTIES.contains(sortBy)) {
            return DEFAULT_SORT_PROPERTY;
        }
        return sortBy;
    }

    private static Sort.Direction resolveDirection(Sort.Direction direction) {
        return direction == null ? DEFAULT_SORT_DIRECTION : direction;
    }
}
