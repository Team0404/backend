package com.sparta.company.product.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Product 도메인 전용 에러코드. 접두사 'P' + 100번대 사용.
 */
@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCodeIfs {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P101", "존재하지 않는 상품입니다."),
    PRODUCT_ALREADY_DELETED(HttpStatus.CONFLICT, "P102", "이미 삭제된 상품입니다."),
    INVALID_COMPANY_ID(HttpStatus.BAD_REQUEST, "P103", "존재하지 않거나 삭제된 업체입니다."),
    FORBIDDEN_HUB_SCOPE(HttpStatus.FORBIDDEN, "P104", "담당 허브에 소속된 상품이 아닙니다."),
    FORBIDDEN_COMPANY_SCOPE(HttpStatus.FORBIDDEN, "P105", "본인 소속 업체의 상품이 아닙니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "P106", "재고가 부족합니다."),
    STOCK_LOCK_TIMEOUT(HttpStatus.CONFLICT, "P107", "재고 처리가 지연되고 있습니다. 잠시 후 다시 시도해주세요."),
    USER_INFO_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "P108", "사용자 정보를 확인할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
