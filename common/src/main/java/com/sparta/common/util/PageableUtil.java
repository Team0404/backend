package com.sparta.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * 목록 API 페이징 요청값 정규화 유틸.
 *
 * <p>클라이언트가 요청한 {@link Pageable} 의 page와 sort는 유지하되,
 * API 정책에서 허용한 size({@code 10}, {@code 30}, {@code 50})만 Repository에 전달한다.
 * 허용되지 않은 size는 기본값 {@code 10}으로 보정한다.
 *
 * <pre>
 * page=2&amp;size=25&amp;sort=createdAt,desc
 * → page=2&amp;size=10&amp;sort=createdAt,desc
 * </pre>
 */
public final class PageableUtil {

    private static final int DEFAULT_SIZE = 10;
    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);

    private PageableUtil() {
    }

    public static Pageable normalize(Pageable pageable) {
        int size = ALLOWED_SIZES.contains(pageable.getPageSize())
                ? pageable.getPageSize()
                : DEFAULT_SIZE;

        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
    }
}
