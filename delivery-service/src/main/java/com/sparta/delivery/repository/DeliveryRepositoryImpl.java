package com.sparta.delivery.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.delivery.domain.entity.Delivery;
import com.sparta.delivery.domain.entity.QDelivery;
import com.sparta.delivery.repository.query.DeliverySearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryRepositoryCustom {

    private static final QDelivery DELIVERY = QDelivery.delivery;
    private static final Map<String, ComparableExpressionBase<?>> SORT_FIELD_MAP = Map.of(
            "createdAt", DELIVERY.createdAt,
            "updatedAt", DELIVERY.updatedAt,
            "status", DELIVERY.status,
            "destHubId", DELIVERY.destHubId,
            "recipientName", DELIVERY.recipientName
    );

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Delivery> search(DeliverySearchCriteria criteria, Pageable pageable) {
        BooleanBuilder predicate = buildPredicate(criteria);

        List<Delivery> content = queryFactory
                .selectFrom(DELIVERY)
                .where(predicate)
                .orderBy(resolveOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(DELIVERY.count())
                .from(DELIVERY)
                .where(predicate);

        return new PageImpl<>(content, pageable, countQuery.fetchOne());
    }

    private BooleanBuilder buildPredicate(DeliverySearchCriteria criteria) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(DELIVERY.deletedAt.isNull());

        if (criteria.status() != null) {
            builder.and(DELIVERY.status.eq(criteria.status()));
        }
        if (criteria.destHubId() != null) {
            builder.and(DELIVERY.destHubId.eq(criteria.destHubId()));
        }
        if (criteria.recipientName() != null && !criteria.recipientName().isBlank()) {
            builder.and(DELIVERY.recipientName.contains(criteria.recipientName()));
        }
        if (criteria.scopeHubId() != null) {
            builder.and(DELIVERY.originHubId.eq(criteria.scopeHubId())
                    .or(DELIVERY.destHubId.eq(criteria.scopeHubId())));
        }
        if (criteria.scopeManagerId() != null) {
            builder.and(scopeManagerCondition(criteria));
        }

        return builder;
    }

    private BooleanExpression scopeManagerCondition(DeliverySearchCriteria criteria) {
        BooleanExpression condition = DELIVERY.companyDeliveryManagerId.eq(criteria.scopeManagerId());

        List<java.util.UUID> routeIds = criteria.scopeRouteDeliveryIds();
        if (routeIds != null && !routeIds.isEmpty()) {
            condition = condition.or(DELIVERY.deliveryId.in(routeIds));
        }
        return condition;
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
