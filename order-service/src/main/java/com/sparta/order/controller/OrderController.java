package com.sparta.order.controller;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.order.dto.request.CreateOrderRequest;
import com.sparta.order.dto.request.OrderSearchRequest;
import com.sparta.order.dto.request.UpdateOrderRequest;
import com.sparta.order.dto.request.UpdateOrderStatusRequest;
import com.sparta.order.dto.response.OrderResponse;
import com.sparta.order.service.OrderService;
import com.sparta.order.service.OrderServiceContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "주문 관리 API")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "주문 생성",
            description = "주문을 생성하고 재고 차감 및 배송 생성을 함께 처리합니다. 허용 권한: MASTER, HUB_MANAGER, SUPPLIER_MANAGER"
    )
    @PostMapping
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('SUPPLIER_MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        OrderResponse response = orderService.createOrder(
                request,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문 생성이 완료되었습니다.", response));
    }

    @Operation(
            summary = "주문 목록 조회",
            description = "권한에 따라 조회 범위가 달라지는 주문 목록 조회 API입니다."
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @Valid @ModelAttribute OrderSearchRequest request,
            Pageable pageable,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        PageResponse<OrderResponse> response = orderService.getOrders(
                request,
                pageable,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "권한에 따라 접근 가능한 주문인지 검사한 뒤 상세 정보를 조회합니다."
    )
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        OrderResponse response = orderService.getOrder(
                orderId,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ApiResponse.success(response);
    }

    @Operation(
            summary = "주문 수정",
            description = "요청사항, 납기일 등 일반 주문 정보를 수정합니다. 허용 권한: MASTER, HUB_MANAGER, SUPPLIER_MANAGER"
    )
    @PatchMapping("/{orderId}")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('SUPPLIER_MANAGER')")
    public ApiResponse<OrderResponse> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderRequest request,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        OrderResponse response = orderService.updateOrder(
                orderId,
                request,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ApiResponse.success("주문 수정이 완료되었습니다.", response);
    }

    @Operation(
            summary = "주문 상태 변경",
            description = "주문 상태를 별도 API로 변경합니다. 허용 권한: MASTER, HUB_MANAGER"
    )
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        OrderResponse response = orderService.updateOrderStatus(
                orderId,
                request,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ApiResponse.success("주문 상태 변경이 완료되었습니다.", response);
    }

    @Operation(
            summary = "주문 취소",
            description = "주문을 취소하고 재고 복원 및 배송 취소를 함께 처리합니다. 허용 권한: MASTER, HUB_MANAGER, SUPPLIER_MANAGER"
    )
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('SUPPLIER_MANAGER')")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        OrderResponse response = orderService.cancelOrder(
                orderId,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ApiResponse.success("주문 취소가 완료되었습니다.", response);
    }

    @Operation(
            summary = "주문 삭제",
            description = "주문을 Soft Delete 처리합니다. 허용 권한: MASTER, HUB_MANAGER"
    )
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    public ApiResponse<Void> deleteOrder(
            @PathVariable UUID orderId,
            @CurrentUser UserPrincipal currentUser,
            @RequestHeader(value = AuthHeaders.HUB_ID, required = false) UUID requestHubId,
            @RequestHeader(value = AuthHeaders.COMPANY_ID, required = false) UUID requestCompanyId,
            @RequestHeader(value = AuthHeaders.DELIVERY_MANAGER_ID, required = false) UUID requestDeliveryManagerId
    ) {
        orderService.deleteOrder(
                orderId,
                createContext(currentUser, requestHubId, requestCompanyId, requestDeliveryManagerId)
        );

        return ApiResponse.success("주문 삭제가 완료되었습니다.", null);
    }

    private OrderServiceContext createContext(
            UserPrincipal currentUser,
            UUID requestHubId,
            UUID requestCompanyId,
            UUID requestDeliveryManagerId
    ) {
        return new OrderServiceContext(
                currentUser.getUserId(),
                currentUser.getRole(),
                requestHubId,
                requestCompanyId,
                requestDeliveryManagerId
        );
    }
}
