package com.sparta.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.web.bind.annotation.PostMapping;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;

/**
 * 컨트롤러 파라미터에 붙여 현재 로그인 사용자(UserPrincipal)를 주입받는다.
 * 예) public ... me(@CurrentUser UserPrincipal principal)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}