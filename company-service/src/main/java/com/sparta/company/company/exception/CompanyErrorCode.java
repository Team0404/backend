package com.sparta.company.company.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CompanyErrorCode implements ErrorCodeIfs {

    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "C101", "존재하지 않는 업체입니다."),
    COMPANY_ALREADY_DELETED(HttpStatus.CONFLICT, "C102", "이미 삭제된 업체입니다."),
    INVALID_HUB_ID(HttpStatus.BAD_REQUEST, "C103", "존재하지 않는 허브입니다."),
    FORBIDDEN_HUB_SCOPE(HttpStatus.FORBIDDEN, "C104", "담당 허브에 소속된 업체가 아닙니다."),
    FORBIDDEN_COMPANY_SCOPE(HttpStatus.FORBIDDEN, "C105", "본인 소속 업체가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
