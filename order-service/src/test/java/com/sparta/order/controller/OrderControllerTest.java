package com.sparta.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.common.config.SecurityConfig;
import com.sparta.common.config.WebConfig;
import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.dto.request.UpdateOrderRequest;
import com.sparta.order.dto.request.UpdateOrderStatusRequest;
import com.sparta.order.dto.response.OrderItemResponse;
import com.sparta.order.dto.response.OrderResponse;
import com.sparta.order.entity.OrderStatus;
import com.sparta.order.service.OrderService;
import com.sparta.order.service.OrderServiceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, WebConfig.class})
class OrderControllerTest {

    private static final String BASE_URL = "/api/v1/orders";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("주문 생성 API는 201과 주문 응답을 반환한다")
    void createOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID deliveryManagerId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                companyId,
                List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 3)),
                "문 앞에 놓아주세요",
                LocalDateTime.now().plusDays(1)
        );

        OrderResponse response = sampleOrderResponse(UUID.randomUUID(), companyId, hubId);
        given(orderService.createOrder(any(CreateOrderRequest.class), any(OrderServiceContext.class)))
                .willReturn(response);
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "supplier", UserRole.SUPPLIER_MANAGER));

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AuthHeaders.USER_ID, userId)
                .header(AuthHeaders.USERNAME, "supplier")
                .header(AuthHeaders.USER_ROLE, UserRole.SUPPLIER_MANAGER.name())
                        .header(AuthHeaders.HUB_ID, hubId)
                        .header(AuthHeaders.COMPANY_ID, companyId)
                        .header(AuthHeaders.DELIVERY_MANAGER_ID, deliveryManagerId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 생성이 완료되었습니다."))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()));

        ArgumentCaptor<OrderServiceContext> contextCaptor = ArgumentCaptor.forClass(OrderServiceContext.class);
        verify(orderService).createOrder(eq(request), contextCaptor.capture());
        assertThat(contextCaptor.getValue().requestUserId()).isEqualTo(userId);
        assertThat(contextCaptor.getValue().userRole()).isEqualTo(UserRole.SUPPLIER_MANAGER);
        assertThat(contextCaptor.getValue().requestHubId()).isEqualTo(hubId);
        assertThat(contextCaptor.getValue().requestCompanyId()).isEqualTo(companyId);
        assertThat(contextCaptor.getValue().requestDeliveryManagerId()).isEqualTo(deliveryManagerId);
    }

    @Test
    @DisplayName("주문 목록 조회 API는 페이지 응답을 반환한다")
    void getOrders() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        OrderResponse orderResponse = sampleOrderResponse(UUID.randomUUID(), companyId, hubId);
        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(orderResponse),
                0,
                10,
                1L,
                1,
                false
        );

        given(orderService.getOrders(any(), any(Pageable.class), any(OrderServiceContext.class)))
                .willReturn(pageResponse);
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "master", UserRole.MASTER));

        mockMvc.perform(get(BASE_URL)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "master")
                        .header(AuthHeaders.USER_ROLE, UserRole.MASTER.name())
                        .param("companyId", companyId.toString())
                        .param("status", OrderStatus.READY.name())
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("주문 상세 조회 API는 주문 응답을 반환한다")
    void getOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        given(orderService.getOrder(eq(orderId), any(OrderServiceContext.class)))
                .willReturn(sampleOrderResponse(orderId, companyId, hubId));
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "hub-manager", UserRole.HUB_MANAGER));

        mockMvc.perform(get(BASE_URL + "/{orderId}", orderId)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "hub-manager")
                        .header(AuthHeaders.USER_ROLE, UserRole.HUB_MANAGER.name())
                        .header(AuthHeaders.HUB_ID, hubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()));
    }

    @Test
    @DisplayName("주문 수정 API는 수정된 주문 응답을 반환한다")
    void updateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        UpdateOrderRequest request = new UpdateOrderRequest(
                "요청사항 수정",
                LocalDateTime.now().plusDays(2),
                OrderStatus.CONFIRMED
        );

        given(orderService.updateOrder(eq(orderId), any(UpdateOrderRequest.class), any(OrderServiceContext.class)))
                .willReturn(sampleOrderResponse(orderId, companyId, hubId));
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "supplier", UserRole.SUPPLIER_MANAGER));

        mockMvc.perform(patch(BASE_URL + "/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "supplier")
                        .header(AuthHeaders.USER_ROLE, UserRole.SUPPLIER_MANAGER.name())
                        .header(AuthHeaders.COMPANY_ID, companyId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 수정이 완료되었습니다."));
    }

    @Test
    @DisplayName("주문 상태 변경 API는 변경된 주문 응답을 반환한다")
    void updateOrderStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED);

        given(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class), any(OrderServiceContext.class)))
                .willReturn(sampleOrderResponse(orderId, companyId, hubId));
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "hub-manager", UserRole.HUB_MANAGER));

        mockMvc.perform(patch(BASE_URL + "/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "hub-manager")
                        .header(AuthHeaders.USER_ROLE, UserRole.HUB_MANAGER.name())
                        .header(AuthHeaders.HUB_ID, hubId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 상태 변경이 완료되었습니다."));
    }

    @Test
    @DisplayName("주문 취소 API는 취소된 주문 응답을 반환한다")
    void cancelOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        given(orderService.cancelOrder(eq(orderId), any(OrderServiceContext.class)))
                .willReturn(sampleOrderResponse(orderId, companyId, hubId));
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "supplier", UserRole.SUPPLIER_MANAGER));

        mockMvc.perform(patch(BASE_URL + "/{orderId}/cancel", orderId)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "supplier")
                        .header(AuthHeaders.USER_ROLE, UserRole.SUPPLIER_MANAGER.name())
                        .header(AuthHeaders.COMPANY_ID, companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 취소가 완료되었습니다."));
    }

    @Test
    @DisplayName("주문 삭제 API는 성공 응답을 반환한다")
    void deleteOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();

        doNothing().when(orderService).deleteOrder(eq(orderId), any(OrderServiceContext.class));
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "hub-manager", UserRole.HUB_MANAGER));

        mockMvc.perform(delete(BASE_URL + "/{orderId}", orderId)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "hub-manager")
                        .header(AuthHeaders.USER_ROLE, UserRole.HUB_MANAGER.name())
                        .header(AuthHeaders.HUB_ID, hubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문 삭제가 완료되었습니다."));
    }

    @Test
    @DisplayName("허용되지 않은 Role은 주문 생성 API를 호출할 수 없다")
    void createOrderForbiddenRole() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                companyId,
                List.of(new CreateOrderRequest.OrderItemRequest(UUID.randomUUID(), 3)),
                "문 앞에 놓아주세요",
                LocalDateTime.now().plusDays(1)
        );
        SecurityContextHolder.getContext().setAuthentication(userAuthentication(userId, "delivery", UserRole.DELIVERY_MANAGER));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AuthHeaders.USER_ID, userId)
                        .header(AuthHeaders.USERNAME, "delivery")
                        .header(AuthHeaders.USER_ROLE, UserRole.DELIVERY_MANAGER.name())
                        .header(AuthHeaders.COMPANY_ID, companyId)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private OrderResponse sampleOrderResponse(UUID orderId, UUID companyId, UUID hubId) {
        UUID deliveryId = UUID.randomUUID();

        return new OrderResponse(
                orderId,
                "ORD-12345678",
                companyId,
                hubId,
                deliveryId,
                "요청사항",
                LocalDateTime.of(2026, 8, 6, 12, 0),
                OrderStatus.READY,
                List.of(new OrderItemResponse(UUID.randomUUID(), UUID.randomUUID(), "상품명", 12000L, 2)),
                LocalDateTime.of(2026, 8, 5, 9, 0),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 5, 10, 0),
                UUID.randomUUID()
        );
    }

    private UsernamePasswordAuthenticationToken userAuthentication(UUID userId, String username, UserRole role) {
        UserPrincipal principal = new UserPrincipal(userId, username, role);
        return new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
