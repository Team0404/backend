package com.sparta.order.config;

import com.sparta.common.constant.AuthHeaders;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 게이트웨이에서 넘어온 인증 헤더들을 추출하여 Feign 요청에 추가 (헤더 전파)
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
}
