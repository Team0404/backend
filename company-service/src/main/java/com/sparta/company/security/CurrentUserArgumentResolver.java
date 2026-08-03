package com.sparta.company.security;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.company.security.AuthUser;
import com.sparta.company.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String userId = request.getHeader(AuthHeaders.USER_ID);
        String username = request.getHeader(AuthHeaders.USERNAME);
        String role = request.getHeader(AuthHeaders.USER_ROLE);

        if (userId == null || role == null) {
            // Gateway를 거치지 않고 직접 호출된 경우 등, 인증 헤더가 없으면 인증 실패로 처리
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return new AuthUser(userId, username, role);
    }
}
