package com.sparta.common.security;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * {@code @CurrentUser UserPrincipal} 파라미터를 게이트웨이 주입 헤더로 채운다.
 * 헤더가 없으면(게이트웨이 미경유/비인증) UNAUTHORIZED 로 처리한다.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(UserPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String userId = webRequest.getHeader(AuthHeaders.USER_ID);
        String username = webRequest.getHeader(AuthHeaders.USERNAME);
        String role = webRequest.getHeader(AuthHeaders.USER_ROLE);

        if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return new UserPrincipal(UUID.fromString(userId), username, UserRole.valueOf(role));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
