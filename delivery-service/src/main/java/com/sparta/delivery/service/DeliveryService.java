package com.sparta.delivery.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.util.PageableUtil;
import com.sparta.delivery.client.HubClient;
import com.sparta.delivery.client.OrderClient;
import com.sparta.delivery.client.UserClient;
import com.sparta.delivery.client.dto.HubRouteResponseDto;
import com.sparta.delivery.domain.dto.request.DeliveryCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryRouteUpdateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.*;
import com.sparta.delivery.domain.entity.*;
import com.sparta.delivery.exception.DeliveryErrorCode;
import com.sparta.delivery.exception.DeliveryManagerErrorCode;
import com.sparta.delivery.repository.DeliveryManagerCursorRepository;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import com.sparta.delivery.repository.DeliveryRepository;
import com.sparta.delivery.repository.DeliveryRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 배송(D1~D7) 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final DeliveryManagerRepository deliveryManagerRepository;
    private final DeliveryManagerCursorRepository deliveryManagerCursorRepository;
    private final UserClient userClient;
    private final OrderClient orderClient;
    private final HubClient hubClient;

    // TODO: HubClient(허브 간 경로 조회), UserClient(허브소속/주문소유 확인) — @FeignClient interface 로 추가

    /**
     * D1. 배송 생성 (내부: 주문서비스 or MASTER).
     */
    @Transactional
    public ApiResponse<DeliveryCreateResponseDto> createDelivery(DeliveryCreateRequestDto request, UUID userId, UserRole role, String internalCaller) {
        // TODO 혀용 서비스 목록 Set.contain - 서비스여도 좋고 common도 좋고
        boolean isOrderService = "order-service".equals(internalCaller);
        if (!UserRole.MASTER.equals(role) && !isOrderService) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if(deliveryRepository.existsByOrderId(request.getOrderId())){
            throw new BusinessException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
        }

        UUID assignedId = assignNextManager(DeliveryManagerType.COMPANY, request.getDestHubId());

        Delivery delivery = Delivery.builder()
                .orderId(request.getOrderId())
                .originHubId(request.getOriginHubId())
                .destHubId(request.getDestHubId())
                .deliveryAddress(request.getDeliveryAddress())
                .recipientName(request.getRecipientName())
                .recipientSlackId(request.getRecipientSlackId())
                .companyDeliveryManagerId(assignedId)
                .build();

        // TODO 동시성 고려
        Delivery savedDelivery;
        try {
            savedDelivery = deliveryRepository.saveAndFlush(delivery);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS);
        }
        // TODO: HubClient로 origin→dest 경로 조회 / 구간 수 만큼 DeliveryRoute 생성
//        ApiResponse<List<HubRouteResponseDto>> routeOriginToDest = hubClient.getRouteOriginToDest();
//        if(!routeOriginToDest.isSuccess()) {
//            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR);
//        }

        List<DeliveryRoute> drList = new ArrayList<>();
//        AtomicInteger routeSequence = new AtomicInteger();
//        List<DeliveryRoute> drList = routeOriginToDest.getData().stream()
//                .map(dto -> DeliveryRoute.builder()
//                        .delivery(savedDelivery)
//                        .sequence(routeSequence.getAndIncrement())
//                        .originHubId(dto.getDeparture_hub_id())
//                        .destHubId(dto.getArrival_hub_id())
//                        .expectedDistanceKm(dto.getDistance_km())
//                        .expectedDurationMin(dto.getDuration_minutes())
//                        // TODO: N번 반복 호출 -> 개선 필요
//                        .hubDeliveryManagerId(assignNextManager(DeliveryManagerType.HUB, dto.getDeparture_hub_id()))
//                        .build())
//                .toList();
        deliveryRouteRepository.saveAll(drList);

        return ApiResponse.success(
                DeliveryCreateResponseDto.builder()
                        .deliveryId(savedDelivery.getDeliveryId())
                        .status(String.valueOf(savedDelivery.getStatus()))
                        .routeCount(drList.size())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse<DeliveryFindResponseDto> findDelivery(UUID deliveryId, UUID userId, UserRole role) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(
                () -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND)
        );
        authorizeDeliveryAccess(delivery, userId, role);
        return ApiResponse.success(DeliveryFindResponseDto.builder()
                .deliveryId(deliveryId)
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                .originHubId(delivery.getOriginHubId())
                .destHubId(delivery.getDestHubId())
                .deliveryAddress(delivery.getDeliveryAddress())
                .recipientName(delivery.getRecipientName())
                .recipientSlackId(delivery.getRecipientSlackId())
                .companyDeliveryManagerId(delivery.getCompanyDeliveryManagerId())
                .build()
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<DeliverySummaryResponseDto>> searchDelivery(
            String status, UUID destHubId, String recipientName, Pageable pageable, UUID userId, UserRole role) {
        Pageable normalized = PageableUtil.normalize(pageable);

        DeliveryStatusEnum statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = DeliveryStatusEnum.fromString(status);
        }

        UUID scopeHubId = null;
        UUID scopeManagerId = null;
        List<UUID> scopeRouteDeliveryIds = List.of();

        switch (role) {
            case MASTER -> {
            }
            case HUB_MANAGER -> {
                ApiResponse<UserInfoResponse> userResponse = userClient.getUser(userId);
                if (!userResponse.isSuccess()) {
                    throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "Delivery -> User 조회 실패");
                }
                scopeHubId = userResponse.getData().getHubId();
            }
            case DELIVERY_MANAGER -> {
                scopeManagerId = userId;
                scopeRouteDeliveryIds = deliveryRouteRepository.findDeliveryIdByHubDeliveryManagerId(userId);
            }
            // TODO: OrderClient 본인 주문 목록 조회 API 필요
            case SUPPLIER_MANAGER -> throw new UnsupportedOperationException("TODO: OrderClient 본인 주문 목록 조회 API 필요");
        }

        Page<Delivery> page = deliveryRepository.search(
                statusEnum, destHubId, recipientName, scopeHubId, scopeManagerId, scopeRouteDeliveryIds, normalized
        );

        return ApiResponse.success(PageResponse.from(page.map(d -> DeliverySummaryResponseDto.builder()
                .deliveryId(d.getDeliveryId())
                .orderId(d.getOrderId())
                .status(d.getStatus())
                .destHubId(d.getDestHubId())
                .recipientName(d.getRecipientName())
                .build())));
    }

    @Transactional
    public ApiResponse<DeliveryUpdateResponseDto> updateDelivery(
            UUID deliveryId, DeliveryUpdateRequestDto request, UUID userId, UserRole role) {
        Delivery delivery = deliveryRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId).orElseThrow(
                () -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND)
        );
        authorizeDeliveryUpdate(delivery, userId, role);
        delivery.update(
                request.getStatus(), request.getDeliveryAddress(),
                request.getRecipientName(), request.getRecipientSlackId(),
                request.getCompanyDeliveryManagerId()
                );
        return ApiResponse.success(DeliveryUpdateResponseDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .status(delivery.getStatus())
                .build());
    }

    @Transactional
    public ApiResponse<Void> deleteDelivery(UUID deliveryId, UUID userId, UserRole role, String internalCaller) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(
                () -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND)
        );

        boolean isOrderService = "order-service".equals(internalCaller);
        if (!isOrderService) {
            authorizeDeliveryDelete(delivery, userId, role);
        }

        delivery.softDelete(userId);
        deliveryRouteRepository.findAllByDeliveryOrderBySequenceAsc(delivery)
                .forEach(route -> route.softDelete(userId));

        return ApiResponse.success(null);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DeliveryRouteSearchResponseDto>> findDeliveryRoutes(
            UUID deliveryId, UUID userId, UserRole role) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElseThrow(
                () -> new BusinessException(DeliveryErrorCode.DELIVERY_NOT_FOUND)
        );
        authorizeDeliveryAccess(delivery, userId, role);

        List<DeliveryRouteSearchResponseDto> routes = deliveryRouteRepository
                .findAllByDeliveryOrderBySequenceAsc(delivery).stream()
                .map(r -> DeliveryRouteSearchResponseDto.builder()
                        .deliveryRouteId(r.getId())
                        .sequence(r.getSequence())
                        .originHubId(r.getOriginHubId())
                        .destHubId(r.getDestHubId())
                        .expectedDistanceKm(r.getExpectedDistanceKm())
                        .expectedDurationMin(r.getExpectedDurationMin())
                        .actualDistanceKm(r.getActualDistanceKm())
                        .actualDurationMin(r.getActualDurationMin())
                        .status(String.valueOf(r.getStatus()))
                        .hubDeliveryManagerId(r.getHubDeliveryManagerId())
                        .build())
                .toList();

        return ApiResponse.success(routes);
    }

    @Transactional
    public ApiResponse<DeliveryRouteUpdateResponseDto> updateDeliveryRoute(
            UUID deliveryRouteId, DeliveryRouteUpdateRequestDto request, UUID userId, UserRole role) {
        DeliveryRoute route = deliveryRouteRepository.findById(deliveryRouteId).orElseThrow(
                () -> new BusinessException(DeliveryErrorCode.DELIVERY_ROUTE_NOT_FOUND)
        );
        authorizeRouteAccess(route, userId, role);
        route.update(
                request.getStatus(), request.getActualDistanceKm(),
                request.getActualDurationMin(), request.getHubDeliveryManagerId()
        );
        return ApiResponse.success(DeliveryRouteUpdateResponseDto.builder()
                .deliveryRouteId(route.getId())
                .status(route.getStatus())
                .build());
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    /**
     * role별 배송 접근 범위 검증.
     *  - SUPPLIER_MANAGER: delivery.orderId 가 본인 주문인지 (Order 서비스 Feign)
     *  실패 시 ACCESS_DENIED.
     */
    private void authorizeDeliveryAccess(Delivery delivery, UUID userId, UserRole role) {
        switch (role) {
            case MASTER -> {}
            case HUB_MANAGER -> {
                ApiResponse<UserInfoResponse> userResponse = userClient.getUser(userId);
                if (!userResponse.isSuccess()) {
                    throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "Delivery -> User 조회 실패");
                }

                UUID managerHubId = userResponse.getData().getHubId();
                if (managerHubId == null
                        || (!managerHubId.equals(delivery.getDestHubId()) && !managerHubId.equals(delivery.getOriginHubId()))) {
                    throw new BusinessException(DeliveryErrorCode.FORBIDDEN_DELIVERY_SCOPE);
                }
            }
            case DELIVERY_MANAGER -> {
                boolean isCompanyManager = userId.equals(delivery.getCompanyDeliveryManagerId());
                boolean isHubManager = deliveryRouteRepository.findHubDeliveryManagerIdByDelivery(delivery).contains(userId);
                if (!isCompanyManager && !isHubManager) {
                    throw new BusinessException(DeliveryErrorCode.FORBIDDEN_DELIVERY_SCOPE);
                }
            }
            case SUPPLIER_MANAGER -> throw new UnsupportedOperationException("TODO: OrderClient 주문 소유자 확인 API 필요");
        }
    }

    private void authorizeDeliveryUpdate(Delivery delivery, UUID userId, UserRole role) {
        if (role == UserRole.SUPPLIER_MANAGER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        authorizeDeliveryAccess(delivery, userId, role);
    }

    private void authorizeDeliveryDelete(Delivery delivery, UUID userId, UserRole role) {
        if (role == UserRole.SUPPLIER_MANAGER || role == UserRole.DELIVERY_MANAGER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        authorizeDeliveryAccess(delivery, userId, role);
    }

    /**
     * 배송 경로 접근 범위 검증.
     *  - MASTER: 통과
     *  - HUB_MANAGER: userId의 담당 허브 ∈ {route origin/dest hub}
     *  - DELIVERY_MANAGER: 해당 경로의 hubDeliveryManagerId == userId 인 경우만
     *  - SUPPLIER_MANAGER: 접근 불가
     */
    private void authorizeRouteAccess(DeliveryRoute route, UUID userId, UserRole role) {
        switch (role) {
            case MASTER -> {
            }
            case HUB_MANAGER -> {
                ApiResponse<UserInfoResponse> userResponse = userClient.getUser(userId);
                if (!userResponse.isSuccess()) {
                    throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "Delivery -> User 조회 실패");
                }

                UUID managerHubId = userResponse.getData().getHubId();
                if (managerHubId == null
                        || (!managerHubId.equals(route.getOriginHubId()) && !managerHubId.equals(route.getDestHubId()))) {
                    throw new BusinessException(DeliveryErrorCode.FORBIDDEN_DELIVERY_SCOPE);
                }
            }
            case DELIVERY_MANAGER -> {
                if (!userId.equals(route.getHubDeliveryManagerId())) {
                    throw new BusinessException(DeliveryErrorCode.FORBIDDEN_DELIVERY_SCOPE);
                }
            }
            case SUPPLIER_MANAGER -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 라운드로빈 담당자 배정: 그룹(HUB=전체 / COMPANY=해당 hubId)에서 sequence 이용
     * 반환값 = 배정된 담당자 userId.
     */
    @Transactional
    protected UUID assignNextManager(DeliveryManagerType type, UUID hubId) {
        UUID managerHubId = type == DeliveryManagerType.HUB ? null : hubId;
        UUID cursorHubId = type == DeliveryManagerType.HUB ? DeliveryManagerCursor.GLOBAL_HUB_ID : hubId;

        List<DeliveryManager> dmList = deliveryManagerRepository
                .findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(type, managerHubId);
        if (dmList.isEmpty()) {
            return null;
        }

        DeliveryManagerCursor savedCursor = deliveryManagerCursorRepository
                .findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(type, cursorHubId)
                .orElseGet(() -> deliveryManagerCursorRepository.save(DeliveryManagerCursor.builder()
                        .type(type)
                        .hubId(cursorHubId)
                        .lastAssignedManagerId(null)
                        .build()));

        int lastIndex = -1;
        for (int i = 0; i < dmList.size(); i++) {
            if (dmList.get(i).getUserId().equals(savedCursor.getLastAssignedManagerId())) {
                lastIndex = i;
                break;
            }
        }

        UUID nextManagerId = dmList.get((lastIndex + 1) % dmList.size()).getUserId();
        savedCursor.updateLastManager(nextManagerId);
        return nextManagerId;
    }
}
