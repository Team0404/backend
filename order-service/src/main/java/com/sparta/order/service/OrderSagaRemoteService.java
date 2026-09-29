package com.sparta.order.service;

import com.sparta.common.response.ApiResponse;
import com.sparta.order.client.DeliveryClient;
import com.sparta.order.client.ProductClient;
import com.sparta.order.client.dto.DeliveryCreateRequest;
import com.sparta.order.client.dto.DeliveryCreateResponse;
import feign.FeignException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

import static com.sparta.order.config.OrderSagaRetryConfig.DELIVERY_CREATE_RETRY;
import static com.sparta.order.config.OrderSagaRetryConfig.PRODUCT_COMMAND_RETRY;

@Service
@RequiredArgsConstructor
public class OrderSagaRemoteService {

    //Feign ErrorDecoder로 개선할 기술 부채
    private static final String PRODUCT_STOCK_LOCK_TIMEOUT_CODE = "\"P107\"";

    private final ProductClient productClient;
    private final DeliveryClient deliveryClient;
    /**
     * Resilience4j : 분산 시스템의 장애 대응 패턴을 구현하기 위한 라이브러리
     * 직접 for문이나 while문으로 재시도 코드를 짤 수도 있지만, 그러면 예외 분류, 대기 시간, 최대 횟수, 통계 같은 걸 다 직접 관리해야 하기 때문 Resilience4j에게 맡긴다
     * <p>
     * Resilience4j Retry 실행
     */
    private final RetryRegistry retryRegistry;

    public void decreaseStock(java.util.UUID productId, Integer quantity, String referenceId) {
        executeVoid(PRODUCT_COMMAND_RETRY, () ->
                        productClient.decreaseStock(productId, quantity, referenceId),
                "재고 차감 응답이 올바르지 않습니다."
        );
    }

    public void restoreStock(java.util.UUID productId, Integer quantity, String referenceId) {
        executeVoid(PRODUCT_COMMAND_RETRY, () ->
                        productClient.restoreStock(productId, quantity, referenceId),
                "재고 복구 응답이 올바르지 않습니다."
        );
    }

    public DeliveryCreateResponse createDelivery(DeliveryCreateRequest request) {
        return execute(DELIVERY_CREATE_RETRY, () -> requireData(
                invoke(() -> deliveryClient.createDelivery(request), false),
                "배송 생성 응답이 올바르지 않습니다."
        ));
    }

    /**
     * Retry가 해당 로직을 다시 실행해야 하기 때문에
     * 같은 작업을 다시 호출하기 위해 Lambda/Supplier 사용
     *
     * @param retryName
     * @param supplier
     * @param errorMessage
     */
    private void executeVoid(String retryName, Supplier<ApiResponse<Void>> supplier, String errorMessage) {
        execute(retryName, () -> {
            ApiResponse<Void> response = invoke(supplier, true);
            if (response == null || !response.isSuccess()) {
                throw new NonRetryableRemoteException(errorMessage);
            }
            return null;
        });
    }

    /**
     * 실제 retry 발생 구간
     *
     * @param retryName
     * @param supplier
     * @param <T>
     * @return
     */
    private <T> T execute(String retryName, Supplier<T> supplier) {
        Retry retry = retryRegistry.retry(retryName);
        return retry.executeSupplier(supplier);
    }

    /**
     *
     * @param supplier
     * @param allowStockLockTimeoutRetry Product의 409 중에서는 P107만 Retry해야 하지만 Delivery의 409에는 그 규칙을 적용하면 안 되기 때문에
     * @param <T>
     * @return
     */
    private <T> ApiResponse<T> invoke(Supplier<ApiResponse<T>> supplier, boolean allowStockLockTimeoutRetry) {
        try {
            return supplier.get();
        } catch (FeignException exception) {
            throw classifyFeignException(exception, allowStockLockTimeoutRetry);
        }
    }

    /**
     * 오류를 retry 할지 판단
     *
     * @param exception
     * @param allowStockLockTimeoutRetry
     * @return
     */
    private RuntimeException classifyFeignException(FeignException exception, boolean allowStockLockTimeoutRetry) {
        int status = exception.status();

        if (status >= 500 || status == -1) {
            return exception;
        }

        //지금 다른 트랜잭션이 재고를 잡고 있으니까 좀 있다 다시 retry 해보세요
        if (allowStockLockTimeoutRetry && status == 409 && exception.contentUTF8().contains(PRODUCT_STOCK_LOCK_TIMEOUT_CODE)) {
            return exception;
        }

        //400 재고 부족
        //403 승인되지 않은 사용자
        //404 허브를 찾을 수 없음
        //409 중복된 username
        if (status == 400 || status == 403 || status == 404 || status == 409) {
            return new NonRetryableRemoteException("재시도 대상이 아닌 원격 호출 실패입니다.", exception);
        }

        return exception;
    }

    private <T> T requireData(ApiResponse<T> response, String message) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new NonRetryableRemoteException(message);
        }
        return response.getData();
    }
}
