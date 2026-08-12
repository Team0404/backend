package com.sparta.delivery.config;

import com.sparta.common.constant.AuthHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 일반 {@code @Configuration} 빈이라 delivery-service의 모든 Feign 클라이언트에 전역 적용된다
 * (order-service 참고함).
 */
@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        String userId = request.getHeader(AuthHeaders.USER_ID);
        String username = request.getHeader(AuthHeaders.USERNAME);
        String userRole = request.getHeader(AuthHeaders.USER_ROLE);

        if (userId != null) {
            template.header(AuthHeaders.USER_ID, userId);
        }
        if (username != null) {
            template.header(AuthHeaders.USERNAME, username);
        }
        if (userRole != null) {
            template.header(AuthHeaders.USER_ROLE, userRole);
        }
    }
}
