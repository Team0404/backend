package com.sparta.delivery.controller;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.domain.dto.request.DeliveryManagerCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryManagerUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.DeliveryManagerResponseDto;
import com.sparta.delivery.domain.dto.response.DeliveryManagerSearchResponseDto;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.service.DeliveryManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 배송 담당자(DM1~DM5) API.
 *
 * 배송 도메인과 같은 delivery-service 모듈에 두되 클래스만 분리한다.
 * 인증 헤더 처리 방식은 {@link DeliveryController} 와 동일.
 * PathVariable {@code userId}(대상 담당자)와 헤더의 요청자 userId 를 구분하기 위해
 * 대상은 {@code targetUserId} 로 받는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/delivery-managers")
public class DeliveryManagerController {

    private final DeliveryManagerService deliveryManagerService;

    // DM1. 배송 담당자 생성 (MASTER / 담당 허브 HUB_MANAGER)
    @PostMapping
    public ApiResponse<DeliveryManagerResponseDto> createDeliveryManager(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @RequestHeader(AuthHeaders.USER_ROLE) UserRole role,
            @RequestBody @Valid DeliveryManagerCreateRequestDto request
    ) {
        return deliveryManagerService.createDeliveryManager(request, userId, role);
    }

    // DM2. 배송 담당자 단건 조회 (MASTER / 담당 허브 HUB_MANAGER / 본인)
    @GetMapping("/{userId}")
    public ApiResponse<DeliveryManagerResponseDto> findDeliveryManager(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @RequestHeader(AuthHeaders.USER_ROLE) UserRole role,
            @PathVariable("userId") UUID targetUserId
    ) {
        return deliveryManagerService.findDeliveryManager(targetUserId, userId, role);
    }

    // DM3. 배송 담당자 목록/검색 (권한 범위는 서버에서 자동 필터링)
    @GetMapping
    public ApiResponse<DeliveryManagerSearchResponseDto> searchDeliveryManagers(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @RequestHeader(AuthHeaders.USER_ROLE) UserRole role,
            @RequestParam(required = false) DeliveryManagerType type,
            @RequestParam(required = false) UUID hubId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return deliveryManagerService.searchDeliveryManagers(type, hubId, pageable, userId, role);
    }

    // DM4. 배송 담당자 수정 (MASTER / 담당 허브 HUB_MANAGER)
    @PatchMapping("/{userId}")
    public ApiResponse<DeliveryManagerResponseDto> updateDeliveryManager(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @RequestHeader(AuthHeaders.USER_ROLE) UserRole role,
            @PathVariable("userId") UUID targetUserId,
            @RequestBody DeliveryManagerUpdateRequestDto request
    ) {
        return deliveryManagerService.updateDeliveryManager(targetUserId, request, userId, role);
    }

    // DM5. 배송 담당자 삭제 (MASTER / 담당 허브 HUB_MANAGER, Soft Delete)
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteDeliveryManager(
            @RequestHeader(AuthHeaders.USER_ID) UUID userId,
            @RequestHeader(AuthHeaders.USER_ROLE) UserRole role,
            @PathVariable("userId") UUID targetUserId
    ) {
        return deliveryManagerService.deleteDeliveryManager(targetUserId, userId, role);
    }
}
