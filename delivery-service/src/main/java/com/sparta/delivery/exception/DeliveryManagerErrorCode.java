package com.sparta.delivery.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum DeliveryManagerErrorCode implements ErrorCodeIfs {

    DELIVERY_MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "D201", "존재하지 않는 배송 담당자입니다."),
    DELIVERY_MANAGER_ALREADY_EXISTS(HttpStatus.CONFLICT, "D202", "이미 등록된 배송 담당자입니다."),
    NOT_DELIVERY_MANAGER_ROLE(HttpStatus.BAD_REQUEST, "D203", "배송 담당자 역할의 사용자가 아닙니다."),
    INVALID_HUB_ID(HttpStatus.BAD_REQUEST, "D204", "존재하지 않는 허브입니다."),
    INVALID_MANAGER_TYPE_HUB(HttpStatus.BAD_REQUEST, "D205", "담당자 타입과 소속 허브 조합이 올바르지 않습니다."),
    FORBIDDEN_DELIVERY_MANAGER_SCOPE(HttpStatus.FORBIDDEN, "D206", "담당 범위에 속하지 않은 배송 담당자입니다."),
    NO_DELIVERY_MANAGER_FOUND(HttpStatus.NOT_FOUND, "D207", "배송 담당자가 없습니다."),
    DUPLICATE_SEQUENCE(HttpStatus.CONFLICT, "D208", "이미 사용 중인 배송 순번입니다. 다른 순번을 지정해 주세요.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
