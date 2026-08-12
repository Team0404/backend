package com.sparta.order.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.order.entity.Order;
import com.sparta.order.entity.QOrder;
import com.sparta.order.repository.query.OrderSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private static final QOrder ORDER = QOrder.order;
    private static final Map<String, ComparableExpressionBase<?>> SORT_FIELD_MAP = Map.of(
            "createdAt", ORDER.createdAt,
            "updatedAt", ORDER.updatedAt
    );

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> searchOrders(
            OrderSearchCriteria criteria,
            Pageable pageable
    ) {
        BooleanBuilder predicate = buildPredicate(criteria);

        List<Order> content = baseQuery(predicate)
                .orderBy(resolveOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(ORDER.count())
                .from(ORDER)
                .where(predicate)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Optional<Order> findDetailById(OrderSearchCriteria criteria, UUID orderId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(ORDER)
                        .leftJoin(ORDER.orderItems).fetchJoin()
                        .where(
                                ORDER.id.eq(orderId),
                                buildPredicate(criteria)
                        )
                        .fetchOne()
        );
    }

    private JPAQuery<Order> baseQuery(BooleanBuilder predicate) {
        return queryFactory
                .selectFrom(ORDER)
                .where(predicate);
    }

    private BooleanBuilder buildPredicate(OrderSearchCriteria criteria) {
        BooleanBuilder builder = new BooleanBuilder();

        if (criteria.companyId() != null) {
            builder.and(ORDER.companyId.eq(criteria.companyId()));
        }

        if (criteria.status() != null) {
            builder.and(ORDER.status.eq(criteria.status()));
        }

        if (criteria.userRole() == null) {
            return builder.and(ORDER.createdBy.eq(criteria.requestUserId()));
        }

        return switch (criteria.userRole()) {
            case MASTER -> builder;
            case DELIVERY_MANAGER -> {
                // TODO: delivery-service 연동 후 requestDeliveryManagerId 기준으로 담당 배송만 필터링
                // 현재는 담당 배송 매핑 정보가 없어 전체 조회를 막기 위해 결과를 비운다.
                yield builder.and(ORDER.id.isNull());
            }
            case HUB_MANAGER -> {
                if (criteria.requestHubId() != null) {
                    builder.and(ORDER.hubId.eq(criteria.requestHubId()));
                }
                yield builder;
            }
            case SUPPLIER_MANAGER -> {
                if (criteria.requestCompanyId() != null) {
                    builder.and(ORDER.companyId.eq(criteria.requestCompanyId()));
                } else {
                    builder.and(ORDER.createdBy.eq(criteria.requestUserId()));
                }
                yield builder;
            }
        };
    }

    private OrderSpecifier<?>[] resolveOrderSpecifiers(Pageable pageable) {
        return pageable.getSort().stream()
                .map(this::toOrderSpecifier)
                .toArray(OrderSpecifier[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder) {
        ComparableExpressionBase<?> sortField = SORT_FIELD_MAP.get(sortOrder.getProperty());
        if (sortField == null) {
            throw new IllegalArgumentException("Unsupported sort property: " + sortOrder.getProperty());
        }

        return new OrderSpecifier<>(
                sortOrder.isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC,
                sortField
        );
    }
}
