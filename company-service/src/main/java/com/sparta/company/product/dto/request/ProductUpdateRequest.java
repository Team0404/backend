package com.sparta.company.product.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * PATCH 방식 부분 수정 요청. companyId/재고 변경은 지원하지 않음
 * (재고는 /decrease-stock, /restore-stock 전용 API로만 조정).
 */
public record ProductUpdateRequest(
        String name,

        @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
        Long price
) {
}
