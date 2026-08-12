package com.sparta.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCodeIfs {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
