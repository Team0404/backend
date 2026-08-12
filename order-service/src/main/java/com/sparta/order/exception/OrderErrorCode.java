package com.sparta.order.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCodeIfs {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O101", "주문을 찾을 수 없습니다."),
    ACCESSIBLE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O102", "접근 가능한 주문을 찾을 수 없습니다."),
    FORBIDDEN_CREATE_ORDER(HttpStatus.FORBIDDEN, "O103", "주문 생성 권한이 없습니다."),
    FORBIDDEN_READ_ORDER(HttpStatus.FORBIDDEN, "O104", "주문 조회 권한이 없습니다."),
    FORBIDDEN_UPDATE_ORDER(HttpStatus.FORBIDDEN, "O105", "주문 수정 권한이 없습니다."),
    FORBIDDEN_UPDATE_ORDER_STATUS(HttpStatus.FORBIDDEN, "O106", "주문 상태 변경 권한이 없습니다."),
    FORBIDDEN_CANCEL_ORDER(HttpStatus.FORBIDDEN, "O107", "주문 취소 권한이 없습니다."),
    FORBIDDEN_DELETE_ORDER(HttpStatus.FORBIDDEN, "O108", "주문 삭제 권한이 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "O109", "재고가 부족합니다."),
    ORDER_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "O110", "변경할 주문 상태가 필요합니다."),
    ORDER_NOT_UPDATABLE(HttpStatus.BAD_REQUEST, "O111", "현재 상태의 주문은 수정할 수 없습니다."),
    ORDER_STATUS_NOT_CHANGEABLE(HttpStatus.BAD_REQUEST, "O112", "현재 상태의 주문은 상태를 변경할 수 없습니다."),
    ORDER_NOT_CANCELABLE(HttpStatus.BAD_REQUEST, "O113", "현재 상태의 주문은 취소할 수 없습니다."),
    FORBIDDEN_COMPANY_SCOPE(HttpStatus.FORBIDDEN, "O114", "본인 업체 주문만 생성할 수 있습니다."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "O115", "허용되지 않는 주문 상태 변경입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
