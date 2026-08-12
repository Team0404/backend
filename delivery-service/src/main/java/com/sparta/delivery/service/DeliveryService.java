package com.sparta.delivery.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.common.util.PageableUtil;
import com.sparta.delivery.client.AiClient;
import com.sparta.delivery.client.HubClient;
import com.sparta.delivery.client.HubRouteClient;
import com.sparta.delivery.client.OrderClient;
import com.sparta.delivery.client.UserClient;
import com.sparta.delivery.client.dto.AiCancelRequest;
import com.sparta.delivery.client.dto.HubRoutePathResponseDto;
import com.sparta.delivery.client.dto.HubRouteSegmentResponseDto;
import com.sparta.delivery.domain.event.DeliveryCreatedEvent;
import com.sparta.delivery.domain.dto.request.DeliveryCancelRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryRouteUpdateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.*;
import com.sparta.delivery.domain.entity.*;
import com.sparta.delivery.exception.DeliveryErrorCode;
import com.sparta.delivery.repository.DeliveryManagerCursorRepository;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import com.sparta.delivery.repository.DeliveryRepository;
import com.sparta.delivery.repository.DeliveryRouteRepository;
import com.sparta.delivery.repository.query.DeliverySearchCriteria;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 배송(D1~D7) 비즈니스 로직.
 */
@Slf4j
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
    private final HubRouteClient hubRouteClient;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;

    /** 내부 호출(X-Internal-Call)로 배송 생성/취소를 수행할 수 있는 서비스 목록. */
    private static final Set<String> ALLOWED_INTERNAL_SERVICES = Set.of("order-service");

    private boolean isAllowedInternalCaller(String internalCaller) {
        return internalCaller != null && ALLOWED_INTERNAL_SERVICES.contains(internalCaller);
    }

    /**
     * D1. 배송 생성 (내부: 주문서비스 or MASTER).
     */
    @Transactional
    public ApiResponse<DeliveryCreateResponseDto> createDelivery(DeliveryCreateRequestDto request, UUID userId, UserRole role, String internalCaller) {
        if (!UserRole.MASTER.equals(role) && !isAllowedInternalCaller(internalCaller)) {
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
        List<DeliveryRoute> drList = buildDeliveryRoutes(savedDelivery, request.getOriginHubId(), request.getDestHubId());
        deliveryRouteRepository.saveAll(drList);

        publishDeliveryCreatedEvent(request, savedDelivery, drList);

        return ApiResponse.success(
                DeliveryCreateResponseDto.builder()
                        .deliveryId(savedDelivery.getDeliveryId())
                        .status(String.valueOf(savedDelivery.getStatus()))
                        .routeCount(drList.size())
                        .build()
        );
    }

    /**
     * AI 발송시한 알림(A1)을 트리거하는 이벤트를 발행한다.
     *
     * <p>여기서는 발행만 하고, 실제 호출은 커밋 이후 별도 스레드에서 이뤄진다.
     * ({@link DispatchDeadlineEventListener}) AI 알림은 배송 성립의 전제가 아니므로
     * 이 트랜잭션에 묶지 않는다.
     */
    private void publishDeliveryCreatedEvent(
            DeliveryCreateRequestDto request,
            Delivery savedDelivery,
            List<DeliveryRoute> routes
    ) {
        // 알림 대상은 첫 구간을 맡은 허브 배송담당자다. 경로가 아직 생성되지 않는 동안에는
        // 출발 허브 기준으로 직접 배정해 알림 대상만이라도 확보한다.
        UUID hubDeliveryManagerId = routes.isEmpty()
                ? assignNextManager(DeliveryManagerType.HUB, request.getOriginHubId())
                : routes.get(0).getHubDeliveryManagerId();

        List<UUID> waypointHubIds = routes.stream()
                .map(DeliveryRoute::getDestHubId)
                .toList();

        eventPublisher.publishEvent(DeliveryCreatedEvent.builder()
                .orderId(savedDelivery.getOrderId())
                .deliveryId(savedDelivery.getDeliveryId())
                .originHubId(savedDelivery.getOriginHubId())
                .destAddress(savedDelivery.getDeliveryAddress())
                .productInfo(request.getProductInfo())
                .requestNote(request.getRequestNote())
                .hubDeliveryManagerId(hubDeliveryManagerId)
                .waypointHubIds(waypointHubIds)
                .build());
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

        DeliverySearchCriteria criteria = new DeliverySearchCriteria(
                statusEnum, destHubId, recipientName, scopeHubId, scopeManagerId, scopeRouteDeliveryIds
        );
        Page<Delivery> page = deliveryRepository.search(criteria, normalized);

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

    /**
     * 주문 취소에 대한 보상 트랜잭션. order-service의 내부 호출 또는 MASTER의 수동 취소로 진입한다.
     *
     * <p>이미 완료됐거나 이동 중인 구간은 물리적으로 되돌릴 수 없으므로 기록을 그대로 보존하고,
     * 아직 출발하지 않은(HUB_MOVE_WAIT) 구간과 마지막 "허브 → 업체" 배송만 취소한다.
     * 보상 호출은 재시도될 수 있으므로 이미 취소됐거나 배송이 생성되지 않은 경우에도 성공으로 응답한다(멱등).
     */
    @Transactional
    public ApiResponse<DeliveryCancelResponseDto> cancelDelivery(DeliveryCancelRequestDto request, UUID userId, UserRole role, String internalCaller) {
        if (!UserRole.MASTER.equals(role) && !isAllowedInternalCaller(internalCaller)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Delivery delivery = deliveryRepository.findByOrderIdAndDeletedAtIsNull(request.getOrderId())
                .orElse(null);

        // 배송이 아직 생성되지 않았거나 이미 삭제된 주문 — 취소할 대상이 없으므로 성공으로 간주한다.
        if (delivery == null) {
            return ApiResponse.success(DeliveryCancelResponseDto.builder()
                    .status(DeliveryStatusEnum.CANCELLED.name())
                    .cancelledRouteCount(0)
                    .preservedRouteCount(0)
                    .build());
        }

        List<DeliveryRoute> routeList = deliveryRouteRepository.findAllByDeliveryOrderBySequenceAsc(delivery);

        // 이미 취소된 배송에 대한 재시도 — 상태를 다시 건드리지 않고 현재 상태를 그대로 반환한다.
        if (DeliveryStatusEnum.CANCELLED.equals(delivery.getStatus())) {
            return ApiResponse.success(toCancelResponse(delivery, routeList));
        }

        delivery.cancel(request.getReason());

        for (DeliveryRoute route : routeList) {
            route.cancel();
        }

        cancelDispatchDeadline(delivery.getOrderId(), request.getReason());

        return ApiResponse.success(toCancelResponse(delivery, routeList));
    }

    /**
     * AI 발송시한 알림에 대한 보상 처리.
     *
     * <p>아직 발송 전이면 CANCELLED 로 막고, 이미 알림이 나갔으면 slack-service 가 취소 안내를
     * 재발송한다. 슬랙은 Incoming Webhook 이라 보낸 메시지를 지울 수 없어 "롤백"이 성립하지 않는다.
     *
     * <p>실패해도 예외를 전파하지 않는다. 알림 보상이 안 됐다고 배송 취소 자체를 되돌리면
     * 주문 취소 흐름 전체가 막히기 때문이다. 실패 건은 로그로 남긴다.
     */
    private void cancelDispatchDeadline(UUID orderId, String reason) {
        try {
            aiClient.cancelDispatchDeadline(new AiCancelRequest(orderId, reason));
        } catch (Exception e) {
            log.error("AI 발송시한 취소 실패. orderId={}", orderId, e);
        }
    }

    private DeliveryCancelResponseDto toCancelResponse(Delivery delivery, List<DeliveryRoute> routeList) {
        int cancelledRouteCount = (int) routeList.stream()
                .filter(route -> DeliveryRouteStatusEnum.CANCELLED.equals(route.getStatus()))
                .count();

        return DeliveryCancelResponseDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .status(delivery.getStatus().name())
                .cancelledAt(delivery.getCancelledAt())
                .cancelledRouteCount(cancelledRouteCount)
                .preservedRouteCount(routeList.size() - cancelledRouteCount)
                .build();
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
     * hub-service에서 origin→dest 경로(구간 목록)를 조회해 {@link DeliveryRoute} 를 생성한다.
     * 구간마다 허브 배송담당자를 라운드로빈으로 배정한다.
     *
     * <p>origin과 dest가 같은 허브면(상품 창고 허브 = 업체 소속 허브) 경유할 구간이 없으므로
     * 조회 자체를 생략하고 빈 목록을 반환한다.
     */
    private List<DeliveryRoute> buildDeliveryRoutes(Delivery delivery, UUID originHubId, UUID destHubId) {
        if (originHubId.equals(destHubId)) {
            return List.of();
        }

        ApiResponse<HubRoutePathResponseDto> response = hubRouteClient.findRoutePath(originHubId, destHubId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "허브 간 경로 조회에 실패했습니다.");
        }

        List<HubRouteSegmentResponseDto> segments = response.getData().getRoutes();
        if (segments == null || segments.isEmpty()) {
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "허브 간 경로를 찾을 수 없습니다.");
        }

        List<DeliveryRoute> routes = new ArrayList<>();
        int sequence = 0;
        for (HubRouteSegmentResponseDto segment : segments) {
            routes.add(DeliveryRoute.builder()
                    .delivery(delivery)
                    .sequence(sequence++)
                    .originHubId(segment.getDepartureHubId())
                    .destHubId(segment.getArrivalHubId())
                    .expectedDistanceKm(segment.getDistanceKm())
                    .expectedDurationMin(segment.getDurationMinutes())
                    .hubDeliveryManagerId(assignNextManager(DeliveryManagerType.HUB, segment.getDepartureHubId()))
                    .build());
        }
        return routes;
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
