package com.sparta.order.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.saga.retry")
public class OrderSagaRetryProperties {

    private int maxAttempts = 3;
    private long waitDurationMs = 200L;
    private double exponentialBackoffMultiplier = 2.0d;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getWaitDurationMs() {
        return waitDurationMs;
    }

    public void setWaitDurationMs(long waitDurationMs) {
        this.waitDurationMs = waitDurationMs;
    }

    public double getExponentialBackoffMultiplier() {
        return exponentialBackoffMultiplier;
    }

    public void setExponentialBackoffMultiplier(double exponentialBackoffMultiplier) {
        this.exponentialBackoffMultiplier = exponentialBackoffMultiplier;
    }
}
