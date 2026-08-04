package com.sparta.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외의 최상위 타입.
 * ErrorCode를 담아 GlobalExceptionHandler에서 일관되게 처리한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCodeIfs errorCode;

    public BusinessException(ErrorCodeIfs errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCodeIfs errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
