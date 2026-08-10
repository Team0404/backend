package com.sparta.delivery.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.delivery.exception.DeliveryErrorCode;

import java.util.Set;

public enum DeliveryStatusEnum {
    CANCELLED,
    DELIVERED,
    COMPANY_MOVING(Set.of(DELIVERED)),
    OUT_FOR_DELIVERY(Set.of(COMPANY_MOVING)),
    // 이 위의 상태는 배송 취소 불가(업체로 향하고 있기 때문)
    DEST_HUB_ARRIVED(Set.of(CANCELLED, OUT_FOR_DELIVERY)),
    HUB_MOVING(Set.of(CANCELLED, DEST_HUB_ARRIVED)),
    HUB_WAIT(Set.of(CANCELLED, HUB_MOVING)),
    ;

    private final Set<DeliveryStatusEnum> availableSet;

    DeliveryStatusEnum() {
        availableSet = Set.of();
    }

    DeliveryStatusEnum(Set<DeliveryStatusEnum> set) {
        availableSet = set;
    }

    public boolean canTransit(DeliveryStatusEnum nxt) {
        return availableSet.contains(nxt);
    }

    public void validateTransit(DeliveryStatusEnum nxt) {
        if (!canTransit(nxt)) {
            throw new BusinessException(DeliveryErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
    }

    public static DeliveryStatusEnum fromString(String status) {
        try {
            return DeliveryStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 status 입니다.");
        }
    }
}
