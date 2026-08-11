package com.sparta.order.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.response.ApiResponse;
import com.sparta.order.client.CompanyClient;
import com.sparta.order.client.DeliveryClient;
import com.sparta.order.client.ProductClient;
import com.sparta.order.client.dto.CompanyResponse;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.exception.OrderErrorCode;
import com.sparta.order.repository.OrderRepository;
import com.sparta.order.repository.query.OrderSearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("Supplier Manager는 자신의 업체 범위가 아니면 주문 생성이 거부된다")
    void createOrderRejectsDifferentCompanyScope() {
        UUID requestCompanyId = UUID.randomUUID();
        UUID actualCompanyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                requestCompanyId,
                List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 1)),
                "요청",
                LocalDateTime.now().plusDays(1)
        );

        OrderServiceContext context = new OrderServiceContext(
                userId,
                UserRole.SUPPLIER_MANAGER,
                hubId,
                requestCompanyId,
                null
        );

        given(companyClient.getCompany(requestCompanyId))
                .willReturn(ApiResponse.success(new CompanyResponse(actualCompanyId, "업체", hubId, "주소")));

        assertThatThrownBy(() -> orderService.createOrder(request, context))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(OrderErrorCode.FORBIDDEN_COMPANY_SCOPE.getMessage());

        verify(productClient, never()).getProduct(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("접근 범위에 맞지 않는 주문은 조회가 거부된다")
    void getOrderRejectsOutsideScope() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        OrderServiceContext context = new OrderServiceContext(
                userId,
                UserRole.HUB_MANAGER,
                hubId,
                companyId,
                null
        );

        given(orderRepository.findDetailById(any(OrderSearchCriteria.class), eq(orderId)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId, context))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(OrderErrorCode.ACCESSIBLE_ORDER_NOT_FOUND.getMessage());
    }
}
