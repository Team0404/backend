package com.sparta.common.util;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 포맷.
 *
 * <p>Spring Data 의 {@link Page} 를 API 명세의
 * 페이징 응답 규약에 맞춰 변환한다. (PageImpl 직렬화 형태에 의존하지 않기 위함)
 *
 * <pre>
 * { "content": [...], "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "hasNext": false }
 * </pre>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
