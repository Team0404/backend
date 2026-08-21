package com.sparta.order.config;

import com.sparta.order.service.NonRetryableRemoteException;
import com.sparta.order.service.OrderSagaRetryProperties;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OrderSagaRetryConfig {

    public static final String PRODUCT_COMMAND_RETRY = "productCommand";
    public static final String DELIVERY_CREATE_RETRY = "deliveryCreate";

    /**
     * 최대 n번까지 시도하고, 재시도 사이에는 점점 오래 기다리게 한다.
     * 단 NonRetryableRemoteException이면 다시 시도하지 않는다.
     * <p>
     * NonRetryableRemoteException : 재고 부족 등의 이유로 다시 시도 해도 결과가 동일할때
     *
     * @param properties
     * @return
     */
    @Bean
    public RetryRegistry retryRegistry(OrderSagaRetryProperties properties) {
        RetryConfig baseConfig = RetryConfig.custom()
                .maxAttempts(properties.getMaxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(properties.getWaitDurationMs()),
                        properties.getExponentialBackoffMultiplier()
                ))
                .retryOnException(throwable -> !(throwable instanceof NonRetryableRemoteException))
                .build();

        RetryRegistry retryRegistry = RetryRegistry.of(baseConfig);
        retryRegistry.retry(PRODUCT_COMMAND_RETRY, baseConfig);
        retryRegistry.retry(DELIVERY_CREATE_RETRY, baseConfig);
        return retryRegistry;
    }
}
