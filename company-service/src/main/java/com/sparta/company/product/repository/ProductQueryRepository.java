package com.sparta.company.product.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.company.product.dto.request.ProductSearchCondition;
import com.sparta.company.product.entity.Product;
import com.sparta.company.product.entity.QProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private static final QProduct product = QProduct.product;

    private final JPAQueryFactory queryFactory;

    public Page<Product> search(ProductSearchCondition condition, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(product.deletedAt.isNull());

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            builder.and(product.name.containsIgnoreCase(condition.keyword()));
        }
        if (condition.companyId() != null) {
            builder.and(product.company.id.eq(condition.companyId()));
        }
        if (condition.hubId() != null) {
            builder.and(product.hubId.eq(condition.hubId()));
        }

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(resolveOrder(pageable))
                .fetch();

        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private OrderSpecifier<?> resolveOrder(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return product.createdAt.desc();
        }

        Sort.Order order = pageable.getSort().iterator().next();
        boolean asc = order.isAscending();

        return switch (order.getProperty()) {
            case "updatedAt" -> asc ? product.updatedAt.asc() : product.updatedAt.desc();
            default -> asc ? product.createdAt.asc() : product.createdAt.desc();
        };
    }
}
