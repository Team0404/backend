package com.sparta.delivery.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;

public enum DeliveryStatusEnum {
    HUB_WAIT,
    HUB_MOVING,
    DEST_HUB_ARRIVED,
    OUT_FOR_DELIVERY,
    COMPANY_MOVING,
    DELIVERED,
    CANCELLED;

    public static DeliveryStatusEnum fromString(String status){
        // TODO transition 설정
        try {
            return DeliveryStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 status 입니다.");
        }
    }
}
