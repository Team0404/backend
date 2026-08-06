package com.sparta.company.company.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sparta.company.company.dto.request.CompanySearchCondition;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.entity.QCompany;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CompanyQueryRepository {

    private static final QCompany company = QCompany.company;

    private final JPAQueryFactory queryFactory;

    /**
     * keyword(업체명 부분일치), companyType, hubId 조건으로 검색.
     * 삭제되지 않은(deleted_at IS NULL) 데이터만 대상으로 함.
     */
    public Page<Company> search(CompanySearchCondition condition, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(company.deletedAt.isNull());

        if (condition.keyword() != null && !condition.keyword().isBlank()) {
            builder.and(company.name.containsIgnoreCase(condition.keyword()));
        }
        if (condition.companyType() != null) {
            builder.and(company.companyType.eq(condition.companyType()));
        }
        if (condition.hubId() != null) {
            builder.and(company.hubId.eq(condition.hubId()));
        }

        List<Company> content = queryFactory
                .selectFrom(company)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(resolveOrder(pageable))
                .fetch();

        Long total = queryFactory
                .select(company.count())
                .from(company)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /**
     * 정렬 기준은 createdAt, updatedAt만 지원. 그 외 값이 들어오면 기본값(createdAt desc) 사용.
     */
    private OrderSpecifier<?> resolveOrder(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return company.createdAt.desc();
        }

        Sort.Order order = pageable.getSort().iterator().next();
        boolean asc = order.isAscending();

        return switch (order.getProperty()) {
            case "updatedAt" -> asc ? company.updatedAt.asc() : company.updatedAt.desc();
            default -> asc ? company.createdAt.asc() : company.createdAt.desc();
        };
    }
}
