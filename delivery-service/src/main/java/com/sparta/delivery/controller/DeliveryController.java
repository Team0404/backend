package com.sparta.delivery.controller;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.delivery.domain.dto.request.DeliveryCancelRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryRouteUpdateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.*;
import com.sparta.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Delivery", description = "배송 및 배송 경로 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @Operation(
            summary = "배송 생성",
            description = "주문 생성 시 함께 호출되는 내부 API입니다. 허용 권한: MASTER 또는 내부 시스템(주문 서비스)"
    )
    @PostMapping("/deliveries")
    public ResponseEntity<ApiResponse<DeliveryCreateResponseDto>> createDelivery(
            @CurrentUser UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @RequestBody @Valid DeliveryCreateRequestDto request
    ) {
        ApiResponse<DeliveryCreateResponseDto> response = deliveryService.createDelivery(
                request, authUser.getUserId(), authUser.getRole(), internalCaller
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "배송 단건 조회",
            description = "허용 권한: MASTER(전체) / HUB_MANAGER(담당 허브) / DELIVERY_MANAGER(본인 담당) / SUPPLIER_MANAGER(본인 주문)"
    )
    @GetMapping("/deliveries/{deliveryId}")
    public ApiResponse<DeliveryFindResponseDto> findDelivery(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId
    ) {
        return deliveryService.findDelivery(deliveryId, authUser.getUserId(), authUser.getRole());
    }

    @Operation(
            summary = "배송 목록/검색",
            description = "status/destHubId/recipientName으로 필터링하며, 권한에 따라 조회 범위가 서버에서 자동으로 제한됩니다."
    )
    @GetMapping("/deliveries")
    public ApiResponse<PageResponse<DeliverySummaryResponseDto>> searchDelivery(
            @CurrentUser UserPrincipal authUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID destHubId,
            @RequestParam(required = false) String recipientName,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return deliveryService.searchDelivery(status, destHubId, recipientName, pageable, authUser.getUserId(), authUser.getRole());
    }

    @Operation(
            summary = "배송 수정",
            description = "상태 전이, 주소/수령인, 업체 배송담당자 재배정 등을 수정합니다. 허용 권한: MASTER, 담당 허브 HUB_MANAGER, 해당 배송 DELIVERY_MANAGER"
    )
    @PatchMapping("/deliveries/{deliveryId}")
    public ApiResponse<DeliveryUpdateResponseDto> updateDelivery(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId,
            @RequestBody DeliveryUpdateRequestDto request
    ) {
        return deliveryService.updateDelivery(deliveryId, request, authUser.getUserId(), authUser.getRole());
    }

    @Operation(
            summary = "배송 취소",
            description = "주문 취소 시 호출되는 보상 트랜잭션 API입니다. 아직 출발하지 않은 경로와 마지막 업체 배송만 취소하며, "
                    + "이미 완료되었거나 이동 중인 경로는 그대로 보존합니다. 업체 배송이 시작된 이후에는 취소할 수 없습니다. "
                    + "동일 요청이 재시도되어도 안전합니다(멱등). 허용 권한: MASTER 또는 내부 시스템(주문 서비스)"
    )
    @PatchMapping("/deliveries/cancel")
    public ApiResponse<DeliveryCancelResponseDto> cancelDelivery(
            @CurrentUser UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @RequestBody @Valid DeliveryCancelRequestDto request
    ) {
        return deliveryService.cancelDelivery(request, authUser.getUserId(), authUser.getRole(), internalCaller);
    }

    @Operation(
            summary = "배송 삭제",
            description = "Soft Delete 처리되며 연관 배송 경로도 함께 삭제됩니다. 허용 권한: MASTER, 담당 허브 HUB_MANAGER, 또는 내부 시스템(주문 취소)"
    )
    @DeleteMapping("/deliveries/{deliveryId}")
    public ApiResponse<Void> deleteDelivery(
            @CurrentUser UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @PathVariable UUID deliveryId
    ) {
        return deliveryService.deleteDelivery(deliveryId, authUser.getUserId(), authUser.getRole(), internalCaller);
    }

    @Operation(
            summary = "배송 경로 목록 조회",
            description = "배송 단건 조회(D2)와 동일한 권한 범위가 적용됩니다."
    )
    @GetMapping("/deliveries/{deliveryId}/routes")
    public ApiResponse<List<DeliveryRouteSearchResponseDto>> findDeliveryRoutes(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryId
    ) {
        return deliveryService.findDeliveryRoutes(deliveryId, authUser.getUserId(), authUser.getRole());
    }

    @Operation(
            summary = "배송 경로 상태 수정",
            description = "실제 거리/소요시간 기록 및 허브 배송담당자 배정/변경을 포함합니다. 허용 권한: MASTER, 담당 허브 HUB_MANAGER, 해당 경로 DELIVERY_MANAGER(본인)"
    )
    @PatchMapping("/delivery-routes/{deliveryRouteId}")
    public ApiResponse<DeliveryRouteUpdateResponseDto> updateDeliveryRoute(
            @CurrentUser UserPrincipal authUser,
            @PathVariable UUID deliveryRouteId,
            @RequestBody DeliveryRouteUpdateRequestDto request
    ) {
        return deliveryService.updateDeliveryRoute(deliveryRouteId, request, authUser.getUserId(), authUser.getRole());
    }
}
