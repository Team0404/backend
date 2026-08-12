package com.sparta.common.config;

import com.sparta.common.security.CurrentUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * scanBasePackages 에 "com.sparta" 가 포함된(servlet 기반) 서비스에서 자동 적용.
 * {@code @CurrentUser} 파라미터 리졸버를 등록한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
