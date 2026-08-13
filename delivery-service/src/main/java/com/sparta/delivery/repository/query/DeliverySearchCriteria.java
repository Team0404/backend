package com.sparta.delivery.repository.query;

import com.sparta.delivery.domain.entity.DeliveryStatusEnum;

import java.util.List;
import java.util.UUID;

/**
 * D3 검색 조건. scopeHubId/scopeManagerId/scopeRouteDeliveryIds는 role별 조회 범위 제한용
 */
public record DeliverySearchCriteria(
        DeliveryStatusEnum status,
        UUID destHubId,
        String recipientName,
        UUID scopeHubId,
        UUID scopeManagerId,
        List<UUID> scopeRouteDeliveryIds
) {
}
