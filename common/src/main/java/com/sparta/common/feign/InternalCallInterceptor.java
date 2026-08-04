package com.sparta.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;

import static com.sparta.common.constant.AuthHeaders.INTERNAL_CALL;

public class InternalCallInterceptor implements RequestInterceptor {

    private final String serviceName;

    public InternalCallInterceptor(@Value("${spring.application.name}") String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(INTERNAL_CALL, serviceName);
    }
}
