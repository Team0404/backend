package com.sparta.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import static com.sparta.common.constant.AuthHeaders.INTERNAL_CALL;

public class OrderInternalCallInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        template.header(INTERNAL_CALL, "order-service");
    }
}
