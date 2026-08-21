package com.sparta.order.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.response.ApiResponse;
import com.sparta.order.client.CompanyClient;
import com.sparta.order.client.DeliveryClient;
import com.sparta.order.client.ProductClient;
import com.sparta.order.client.UserClient;
import com.sparta.order.client.dto.*;
import com.sparta.order.config.OrderSagaRetryConfig;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.entity.Order;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderSagaRemoteServiceTest {

    @Mock
    private ProductClient productClient;
    @Mock
    private DeliveryClient deliveryClient;
    @Mock
    private CompanyClient companyClient;
    @Mock
    private UserClient userClient;
    @Mock
    private com.sparta.order.repository.OrderRepository orderRepository;

    private OrderSagaRemoteService remoteService;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        OrderSagaRetryProperties properties = new OrderSagaRetryProperties();
        properties.setMaxAttempts(3);
        properties.setWaitDurationMs(1L);
        properties.setExponentialBackoffMultiplier(1.0d);

        retryRegistry = new OrderSagaRetryConfig().retryRegistry(properties);
        remoteService = new OrderSagaRemoteService(productClient, deliveryClient, retryRegistry);
    }

    @Test
    @DisplayName("decreaseStock 첫 호출 timeout 후 재시도 성공 시 같은 referenceId를 재사용한다")
    void decreaseStock_timeoutThenSuccess_reusesSameReferenceId() {
        UUID productId = UUID.randomUUID();
        String referenceId = UUID.randomUUID() + ":DECREASE";

        given(productClient.decreaseStock(eq(productId), eq(1), eq(referenceId)))
                .willThrow(new RuntimeException(new SocketTimeoutException("read timeout")))
                .willReturn(ApiResponse.success(null));

        remoteService.decreaseStock(productId, 1, referenceId);

        ArgumentCaptor<String> referenceCaptor = ArgumentCaptor.forClass(String.class);
        verify(productClient, times(2)).decreaseStock(eq(productId), eq(1), referenceCaptor.capture());
        assertThat(referenceCaptor.getAllValues()).containsExactly(referenceId, referenceId);
    }

    @Test
    @DisplayName("decreaseStock이 재고 부족 400이면 재시도하지 않는다")
    void decreaseStock_badRequest_doesNotRetry() {
        UUID productId = UUID.randomUUID();
        String referenceId = UUID.randomUUID() + ":DECREASE";

        given(productClient.decreaseStock(eq(productId), eq(1), eq(referenceId)))
                .willThrow(feignException(400, "{\"success\":false,\"code\":\"P106\",\"message\":\"재고가 부족합니다.\"}"));

        assertThatThrownBy(() -> remoteService.decreaseStock(productId, 1, referenceId))
                .isInstanceOf(NonRetryableRemoteException.class);

        verify(productClient, times(1)).decreaseStock(productId, 1, referenceId);
    }

    @Test
    @DisplayName("STOCK_LOCK_TIMEOUT(409)은 재시도 후 성공한다")
    void decreaseStock_stockLockTimeout_retriesThenSucceeds() {
        UUID productId = UUID.randomUUID();
        String referenceId = UUID.randomUUID() + ":DECREASE";

        given(productClient.decreaseStock(eq(productId), eq(1), eq(referenceId)))
                .willThrow(feignException(409, "{\"success\":false,\"code\":\"P107\",\"message\":\"재고 처리가 지연되고 있습니다.\"}"))
                .willReturn(ApiResponse.success(null));

        remoteService.decreaseStock(productId, 1, referenceId);

        verify(productClient, times(2)).decreaseStock(productId, 1, referenceId);
    }

    @Test
    @DisplayName("createDelivery 첫 호출 timeout 후 재시도 응답으로 기존 Delivery를 받아도 성공한다")
    void createDelivery_timeoutThenIdempotentSuccess() {
        UUID deliveryId = UUID.randomUUID();
        DeliveryCreateRequest request = new DeliveryCreateRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "주소",
                "수령인",
                "slack-id",
                "상품 1개",
                "요청"
        );

        given(deliveryClient.createDelivery(any(DeliveryCreateRequest.class)))
                .willThrow(new RuntimeException(new SocketTimeoutException("read timeout")))
                .willReturn(ApiResponse.success(new DeliveryCreateResponse(deliveryId, "HUB_WAIT", 0)));

        DeliveryCreateResponse response = remoteService.createDelivery(request);

        assertThat(response.deliveryId()).isEqualTo(deliveryId);
        verify(deliveryClient, times(2)).createDelivery(any(DeliveryCreateRequest.class));
    }

    @Test
    @DisplayName("Retry 대상 오류가 maxAttempts까지 계속 실패하면 createOrder로 예외가 전달되고 기존 보상이 실행된다")
    void createOrder_deliveryRetryExhausted_thenCompensates() {
        UUID supplierCompanyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderServiceImpl orderService = new OrderServiceImpl(
                orderRepository,
                productClient,
                companyClient,
                deliveryClient,
                userClient,
                remoteService
        );

        given(companyClient.getCompany(supplierCompanyId))
                .willReturn(ApiResponse.success(new CompanyResponse(supplierCompanyId, "업체", hubId, "주소")));
        given(productClient.getProduct(productId))
                .willReturn(ApiResponse.success(new ProductResponse(productId, hubId, "상품", 1_000L, 10L)));
        given(userClient.getUser(userId))
                .willReturn(ApiResponse.success(new UserResponse(
                        userId, "user", "nick", "slack-id", UserRole.SUPPLIER_MANAGER, hubId, supplierCompanyId
                )));
        given(productClient.decreaseStock(any(UUID.class), any(Integer.class), any(String.class)))
                .willReturn(ApiResponse.success(null));
        given(productClient.restoreStock(any(UUID.class), any(Integer.class), any(String.class)))
                .willReturn(ApiResponse.success(null));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(deliveryClient.createDelivery(any(DeliveryCreateRequest.class)))
                .willThrow(new RuntimeException(new SocketTimeoutException("read timeout")));

        CreateOrderRequest request = new CreateOrderRequest(
                supplierCompanyId,
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 1)),
                "요청",
                LocalDateTime.now().plusDays(1)
        );

        assertThatThrownBy(() -> orderService.createOrder(request, new OrderServiceContext(
                userId,
                UserRole.SUPPLIER_MANAGER,
                hubId,
                supplierCompanyId,
                null
        ))).isInstanceOf(BusinessException.class);

        verify(deliveryClient, times(3)).createDelivery(any(DeliveryCreateRequest.class));
        verify(productClient, times(1)).restoreStock(eq(productId), eq(1), any(String.class));
    }

    @Test
    @DisplayName("restoreStock 일시 오류는 동일 RESTORE referenceId로 재시도한다")
    void restoreStock_temporaryError_retriesWithSameReferenceId() {
        UUID productId = UUID.randomUUID();
        String referenceId = UUID.randomUUID() + ":RESTORE";

        given(productClient.restoreStock(eq(productId), eq(1), eq(referenceId)))
                .willThrow(new RuntimeException(new SocketTimeoutException("read timeout")))
                .willReturn(ApiResponse.success(null));

        remoteService.restoreStock(productId, 1, referenceId);

        ArgumentCaptor<String> referenceCaptor = ArgumentCaptor.forClass(String.class);
        verify(productClient, times(2)).restoreStock(eq(productId), eq(1), referenceCaptor.capture());
        assertThat(referenceCaptor.getAllValues()).containsExactly(referenceId, referenceId);
    }

    private FeignException feignException(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://localhost/test",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("test", response);
    }
}
