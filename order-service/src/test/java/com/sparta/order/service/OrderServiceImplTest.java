package com.sparta.order.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.response.ApiResponse;
import com.sparta.order.client.CompanyClient;
import com.sparta.order.client.DeliveryClient;
import com.sparta.order.client.ProductClient;
import com.sparta.order.client.UserClient;
import com.sparta.order.client.dto.CompanyResponse;
import com.sparta.order.client.dto.DeliveryCreateResponse;
import com.sparta.order.client.dto.ProductResponse;
import com.sparta.order.client.dto.UserResponse;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.dto.response.OrderResponse;
import com.sparta.order.entity.Order;
import com.sparta.order.entity.OrderItem;
import com.sparta.order.entity.OrderStatus;
import com.sparta.order.exception.OrderErrorCode;
import com.sparta.order.repository.OrderRepository;
import com.sparta.order.repository.query.OrderSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private CompanyClient companyClient;
    @Mock
    private DeliveryClient deliveryClient;
    @Mock
    private UserClient userClient;
    @Mock
    private OrderSagaRemoteService orderSagaRemoteService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID supplierCompanyId;
    private UUID hubId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        supplierCompanyId = UUID.randomUUID();
        hubId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Supplier Manager는 자신의 업체 범위가 아니면 주문 생성이 거부된다")
    void createOrderRejectsDifferentCompanyScope() {
        UUID actualCompanyId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                supplierCompanyId,
                List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1)),
                "요청",
                LocalDateTime.now().plusDays(1)
        );

        given(companyClient.getCompany(supplierCompanyId))
                .willReturn(ApiResponse.success(new CompanyResponse(actualCompanyId, "업체", hubId, "주소")));

        assertThatThrownBy(() -> orderService.createOrder(request, supplierContext()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(OrderErrorCode.FORBIDDEN_COMPANY_SCOPE.getMessage());

        verify(productClient, never()).getProduct(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("접근 범위에 맞지 않는 주문은 조회가 거부된다")
    void getOrderRejectsOutsideScope() {
        UUID orderId = UUID.randomUUID();

        OrderServiceContext context = new OrderServiceContext(
                userId,
                UserRole.HUB_MANAGER,
                hubId,
                supplierCompanyId,
                null
        );

        given(orderRepository.findDetailById(any(OrderSearchCriteria.class), eq(orderId)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId, context))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(OrderErrorCode.ACCESSIBLE_ORDER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("주문 생성 중 배송 생성이 실패하면 차감했던 stockOperationId 기반 RESTORE key로 재고를 복구한다")
    void createOrderCompensationUsesStockOperationRestoreKey() {
        UUID productId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                supplierCompanyId,
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 1)),
                "요청",
                LocalDateTime.now().plusDays(1)
        );

        stubCreateOrderPrerequisites(productId);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderSagaRemoteService.createDelivery(any()))
                .willThrow(new RuntimeException("delivery failed"));

        assertThatThrownBy(() -> orderService.createOrder(request, supplierContext()))
                .isInstanceOf(BusinessException.class);

        ArgumentCaptor<String> decreaseReferenceCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> restoreReferenceCaptor = ArgumentCaptor.forClass(String.class);

        verify(orderSagaRemoteService).decreaseStock(eq(productId), eq(1), decreaseReferenceCaptor.capture());
        verify(orderSagaRemoteService).restoreStock(eq(productId), eq(1), restoreReferenceCaptor.capture());

        String decreaseReferenceId = decreaseReferenceCaptor.getValue();
        String restoreReferenceId = restoreReferenceCaptor.getValue();

        assertThat(decreaseReferenceId).endsWith(":DECREASE");
        assertThat(restoreReferenceId).endsWith(":RESTORE");
        assertThat(restoreReferenceId.replace(":RESTORE", ""))
                .isEqualTo(decreaseReferenceId.replace(":DECREASE", ""));
    }

    @Test
    @DisplayName("저장된 주문 취소 시 OrderItem의 stockOperationId 기반 RESTORE key를 사용한다")
    void cancelOrderUsesPersistedStockOperationRestoreKey() {
        UUID productId = UUID.randomUUID();
        UUID stockOperationId = UUID.randomUUID();
        Order order = Order.builder()
                .orderNumber("ORD-TEST")
                .companyId(supplierCompanyId)
                .hubId(hubId)
                .status(OrderStatus.READY)
                .build();
        order.addOrderItem(OrderItem.builder()
                .productId(productId)
                .productName("상품")
                .unitPrice(1_000L)
                .quantity(2)
                .stockOperationId(stockOperationId)
                .build());

        given(orderRepository.findDetailById(any(OrderSearchCriteria.class), any(UUID.class)))
                .willReturn(Optional.of(order));

        orderService.cancelOrder(UUID.randomUUID(), supplierContext());

        verify(orderSagaRemoteService).restoreStock(
                eq(productId),
                eq(2),
                eq(stockOperationId + ":RESTORE")
        );
    }

    @Test
    @DisplayName("같은 상품이 여러 OrderItem으로 들어와도 각 stockOperationId로 서로 다른 DECREASE key를 사용한다")
    void createOrderUsesDifferentDecreaseKeysForDuplicateProductItems() {
        UUID productId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                supplierCompanyId,
                List.of(
                        new CreateOrderRequest.OrderItemRequest(productId, 1),
                        new CreateOrderRequest.OrderItemRequest(productId, 2)
                ),
                "요청",
                LocalDateTime.now().plusDays(1)
        );

        stubCreateOrderPrerequisites(productId);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderSagaRemoteService.createDelivery(any()))
                .willReturn(new DeliveryCreateResponse(UUID.randomUUID(), "READY", 1));

        orderService.createOrder(request, supplierContext());

        ArgumentCaptor<String> referenceCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderSagaRemoteService, times(2)).decreaseStock(eq(productId), any(Integer.class), referenceCaptor.capture());

        List<String> referenceIds = referenceCaptor.getAllValues();
        assertThat(referenceIds).hasSize(2);
        assertThat(referenceIds.get(0)).endsWith(":DECREASE");
        assertThat(referenceIds.get(1)).endsWith(":DECREASE");
        assertThat(referenceIds.get(0)).isNotEqualTo(referenceIds.get(1));
    }

    @Test
    @DisplayName("이미 존재하는 Delivery의 deliveryId 응답을 받아도 주문 생성 Saga는 성공 흐름으로 진행한다")
    void createOrderSucceedsWhenDeliveryAlreadyExistsResponseReturned() {
        UUID productId = UUID.randomUUID();
        UUID existingDeliveryId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                supplierCompanyId,
                List.of(new CreateOrderRequest.OrderItemRequest(productId, 1)),
                "요청",
                LocalDateTime.now().plusDays(1)
        );

        stubCreateOrderPrerequisites(productId);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderSagaRemoteService.createDelivery(any()))
                .willReturn(new DeliveryCreateResponse(existingDeliveryId, "HUB_WAIT", 0));

        OrderResponse response = orderService.createOrder(request, supplierContext());

        assertThat(response.deliveryId()).isEqualTo(existingDeliveryId);
        assertThat(response.status()).isEqualTo(OrderStatus.READY);
    }

    private void stubCreateOrderPrerequisites(UUID productId) {
        given(companyClient.getCompany(supplierCompanyId))
                .willReturn(ApiResponse.success(new CompanyResponse(supplierCompanyId, "업체", hubId, "주소")));
        given(productClient.getProduct(productId))
                .willReturn(ApiResponse.success(new ProductResponse(
                        productId,
                        hubId,
                        "상품",
                        1_000L,
                        10L
                )));
        given(userClient.getUser(userId))
                .willReturn(ApiResponse.success(new UserResponse(
                        userId,
                        "user",
                        "nick",
                        "slack-id",
                        UserRole.SUPPLIER_MANAGER,
                        hubId,
                        supplierCompanyId
                )));
    }

    private OrderServiceContext supplierContext() {
        return new OrderServiceContext(
                userId,
                UserRole.SUPPLIER_MANAGER,
                hubId,
                supplierCompanyId,
                null
        );
    }
}
