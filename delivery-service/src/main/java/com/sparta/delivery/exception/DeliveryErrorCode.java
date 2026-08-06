package com.sparta.delivery.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 배송(D1~D7) 도메인 전용 에러 코드.
 * 범용 코드(인증/인가 실패, 입력값 검증 등)는 {@link com.sparta.common.exception.ErrorCode}를 사용한다.
 */
@Getter
@RequiredArgsConstructor
public enum DeliveryErrorCode implements ErrorCodeIfs {

    DELIVERY_NOT_FOUND(HttpStatus.NOT_FOUND, "D101", "존재하지 않는 배송입니다."),
    DELIVERY_ALREADY_EXISTS(HttpStatus.CONFLICT, "D102", "이미 배송이 존재하는 주문입니다."),
    DELIVERY_ALREADY_DELETED(HttpStatus.CONFLICT, "D103", "이미 삭제된 배송입니다."),
    FORBIDDEN_DELIVERY_SCOPE(HttpStatus.FORBIDDEN, "D104", "담당 범위에 속하지 않은 배송입니다."),
    INVALID_DELIVERY_STATUS_TRANSITION(HttpStatus.CONFLICT, "D105", "허용되지 않는 배송 상태 전이입니다."),
    DELIVERY_ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "D106", "존재하지 않는 배송 경로입니다."),
    INVALID_DELIVERY_ROUTE_STATUS_TRANSITION(HttpStatus.CONFLICT, "D107", "허용되지 않는 배송 경로 상태 전이입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
