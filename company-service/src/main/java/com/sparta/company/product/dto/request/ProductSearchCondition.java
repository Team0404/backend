package com.sparta.company.product.dto.request;

import java.util.UUID;

public record ProductSearchCondition(
        String keyword,
        UUID companyId,
        UUID hubId
) {
}
