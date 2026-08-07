package com.sparta.delivery.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.delivery.exception.DeliveryErrorCode;

import java.util.Set;

public enum DeliveryRouteStatusEnum {
    CANCELLED,
    // 최종 허브 도착 후 업체로 출발한 상태. 구간의 종착점이며, 이후 흐름은 Delivery 레벨에서 관리한다.
    OUT_FOR_DELIVERY,
    DEST_HUB_ARRIVED(Set.of(OUT_FOR_DELIVERY)),
    HUB_MOVING(Set.of(DEST_HUB_ARRIVED)),
    HUB_MOVE_WAIT(Set.of(CANCELLED, HUB_MOVING)),
    ;

    private final Set<DeliveryRouteStatusEnum> availableSet;

    DeliveryRouteStatusEnum() {
        availableSet = Set.of();
    }

    DeliveryRouteStatusEnum(Set<DeliveryRouteStatusEnum> set) {
        availableSet = set;
    }

    public boolean canTransit(DeliveryRouteStatusEnum nxt) {
        return availableSet.contains(nxt);
    }

    public void validateTransit(DeliveryRouteStatusEnum nxt) {
        if (!canTransit(nxt)) {
            throw new BusinessException(DeliveryErrorCode.INVALID_DELIVERY_ROUTE_STATUS_TRANSITION);
        }
    }
}
