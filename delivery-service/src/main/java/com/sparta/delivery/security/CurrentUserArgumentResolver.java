package com.sparta.delivery.security;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Objects;
import java.util.UUID;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String userIdHeader = request.getHeader(AuthHeaders.USER_ID);
        String username = request.getHeader(AuthHeaders.USERNAME);
        String roleHeader = request.getHeader(AuthHeaders.USER_ROLE);

        if (userIdHeader == null || roleHeader == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        UUID userId;
        UserRole role;
        try {
            userId = UUID.fromString(userIdHeader);
            role = UserRole.valueOf(roleHeader);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return new AuthUser(userId, username, role);
    }
}
