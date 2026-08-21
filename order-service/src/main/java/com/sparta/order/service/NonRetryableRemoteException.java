package com.sparta.order.service;

/**
 * 이 오류는 다시 호출해도 소용없으니까 Retry 하지 마 라고 Resilience4j에게 알려주기 위한 예외
 */
public class NonRetryableRemoteException extends RuntimeException {

    public NonRetryableRemoteException(String message) {
        super(message);
    }

    public NonRetryableRemoteException(String message, Throwable cause) {
        super(message, cause);
    }
}
