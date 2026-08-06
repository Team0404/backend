package com.sparta.delivery.controller;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.delivery.domain.dto.request.DeliveryCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryRouteUpdateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.*;
import com.sparta.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 배송(D1~D7) API.
 *
 * 인증/인가는 Gateway가 JWT 검증 후 전달하는 헤더에 의존한다.
 * 컨트롤러는 {@code X-User-Id}(userId), {@code X-User-Role}(role)만 꺼내
 * 서비스로 넘기고, 실제 권한 범위 판단은 서비스가 수행한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // D1. 배송 생성 (내부: 주문서비스 or MASTER)
    @PostMapping("/deliveries")
    public ApiResponse<DeliveryCreateResponseDto> createDelivery(
            @CurrentUser UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @RequestBody @Valid DeliveryCreateRequestDto request
    ) {
        return deliveryService.createDelivery(request, authUser.getUserId(), authUser.getRole(), internalCaller);
    }

    // D2. 배송 단건 조회
    @GetMapping("/deliveries/{deliveryId}")
    public ApiResponse<DeliveryFindResponseDto> findDelivery(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId
    ) {
        return deliveryService.findDelivery(deliveryId, authUser.getUserId(), authUser.getRole());
    }

    // D3. 배송 목록/검색 (권한 범위는 서버에서 자동 필터링)
    @GetMapping("/deliveries")
    public PageResponse<DeliverySummaryResponseDto> searchDelivery(
            @CurrentUser UserPrincipal authUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID destHubId,
            @RequestParam(required = false) String recipientName,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return deliveryService.searchDelivery(status, destHubId, recipientName, pageable, authUser.getUserId(), authUser.getRole());
    }

    // D4. 배송 수정(상태 등)
    @PatchMapping("/deliveries/{deliveryId}")
    public ApiResponse<DeliveryUpdateResponseDto> updateDelivery(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId,
            @RequestBody DeliveryUpdateRequestDto request
    ) {
        return deliveryService.updateDelivery(deliveryId, request, authUser.getUserId(), authUser.getRole());
    }

    // D5. 배송 삭제 (Soft Delete)
    @DeleteMapping("/deliveries/{deliveryId}")
    public ApiResponse<Void> deleteDelivery(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId
    ) {
        return deliveryService.deleteDelivery(deliveryId, authUser.getUserId(), authUser.getRole());
    }

    // D6. 배송 경로 목록 조회 (D2와 동일 권한)
    @GetMapping("/deliveries/{deliveryId}/routes")
    public ApiResponse<List<DeliveryRouteSearchResponseDto>> findDeliveryRoutes(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId
    ) {
        return deliveryService.findDeliveryRoutes(deliveryId, authUser.getUserId(), authUser.getRole());
    }

    // D7. 배송 경로 상태 수정
    @PatchMapping("/delivery-routes/{deliveryRouteId}")
    public ApiResponse<DeliveryRouteUpdateResponseDto> updateDeliveryRoute(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryRouteId,
            @RequestBody DeliveryRouteUpdateRequestDto request
    ) {
        return deliveryService.updateDeliveryRoute(deliveryRouteId, request, authUser.getUserId(), authUser.getRole());
    }
}
