package com.sparta.delivery.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.delivery.client.HubClient;
import com.sparta.delivery.client.OrderClient;
import com.sparta.delivery.client.UserClient;
import com.sparta.delivery.domain.dto.request.DeliveryCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryRouteUpdateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.DeliveryRouteSearchResponseDto;
import com.sparta.delivery.domain.dto.response.DeliverySummaryResponseDto;
import com.sparta.delivery.domain.entity.Delivery;
import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerCursor;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.domain.entity.DeliveryRoute;
import com.sparta.delivery.domain.entity.DeliveryRouteStatusEnum;
import com.sparta.delivery.exception.DeliveryErrorCode;
import com.sparta.delivery.repository.DeliveryManagerCursorRepository;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import com.sparta.delivery.repository.DeliveryRepository;
import com.sparta.delivery.repository.DeliveryRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryServiceTest {

    private DeliveryRepository deliveryRepository;
    private DeliveryRouteRepository deliveryRouteRepository;
    private DeliveryManagerRepository deliveryManagerRepository;
    private DeliveryManagerCursorRepository deliveryManagerCursorRepository;
    private UserClient userClient;
    private OrderClient orderClient;
    private HubClient hubClient;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(DeliveryRepository.class);
        deliveryRouteRepository = mock(DeliveryRouteRepository.class);
        deliveryManagerRepository = mock(DeliveryManagerRepository.class);
        deliveryManagerCursorRepository = mock(DeliveryManagerCursorRepository.class);
        userClient = mock(UserClient.class);
        orderClient = mock(OrderClient.class);
        hubClient = mock(HubClient.class);
        deliveryService = new DeliveryService(
                deliveryRepository, deliveryRouteRepository, deliveryManagerRepository,
                deliveryManagerCursorRepository, userClient, orderClient, hubClient
        );

        when(deliveryManagerCursorRepository.save(any(DeliveryManagerCursor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createDelivery_masterSuccess_assignsCompanyManagerRoundRobin() {
        UUID destHubId = UUID.randomUUID();
        UUID managerA = UUID.randomUUID();
        UUID managerB = UUID.randomUUID();
        DeliveryCreateRequestDto request = createRequest(destHubId);

        when(deliveryRepository.existsByOrderId(request.getOrderId())).thenReturn(false);
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, destHubId))
                .thenReturn(List.of(companyManager(managerA, destHubId, 0), companyManager(managerB, destHubId, 1)));
        when(deliveryManagerCursorRepository.findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType.COMPANY, destHubId))
                .thenReturn(Optional.empty());
        when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                .thenAnswer(invocation -> {
                    Delivery arg = invocation.getArgument(0);
                    return Delivery.builder()
                            .deliveryId(UUID.randomUUID())
                            .orderId(arg.getOrderId())
                            .originHubId(arg.getOriginHubId())
                            .destHubId(arg.getDestHubId())
                            .deliveryAddress(arg.getDeliveryAddress())
                            .recipientName(arg.getRecipientName())
                            .recipientSlackId(arg.getRecipientSlackId())
                            .companyDeliveryManagerId(arg.getCompanyDeliveryManagerId())
                            .build();
                });

        ApiResponse<?> response = deliveryService.createDelivery(request, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.isSuccess()).isTrue();
        verify(deliveryRepository).saveAndFlush(argThatCompanyManagerIs(managerA));
    }

    @Test
    void createDelivery_emptyManagerPool_companyManagerIsNull() {
        UUID destHubId = UUID.randomUUID();
        DeliveryCreateRequestDto request = createRequest(destHubId);

        when(deliveryRepository.existsByOrderId(request.getOrderId())).thenReturn(false);
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, destHubId))
                .thenReturn(List.of());
        when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        deliveryService.createDelivery(request, UUID.randomUUID(), UserRole.MASTER, null);

        verify(deliveryRepository).saveAndFlush(argThatCompanyManagerIs(null));
    }

    @Test
    void createDelivery_deniedForNonMasterNonInternal() {
        DeliveryCreateRequestDto request = createRequest(UUID.randomUUID());

        assertThatThrownBy(() -> deliveryService.createDelivery(request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void createDelivery_internalOrderServiceCallAllowedRegardlessOfRole() {
        UUID destHubId = UUID.randomUUID();
        DeliveryCreateRequestDto request = createRequest(destHubId);

        when(deliveryRepository.existsByOrderId(request.getOrderId())).thenReturn(false);
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, destHubId))
                .thenReturn(List.of());
        when(deliveryRepository.saveAndFlush(any(Delivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<?> response = deliveryService.createDelivery(request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER, "order-service");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void createDelivery_duplicateOrderId_throwsAlreadyExists() {
        DeliveryCreateRequestDto request = createRequest(UUID.randomUUID());
        when(deliveryRepository.existsByOrderId(request.getOrderId())).thenReturn(true);

        assertThatThrownBy(() -> deliveryService.createDelivery(request, UUID.randomUUID(), UserRole.MASTER, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryErrorCode.DELIVERY_ALREADY_EXISTS));

        verify(deliveryRepository, never()).saveAndFlush(any());
    }

    @Test
    void findDelivery_notFound_throws() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.findDelivery(deliveryId, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }

    @Test
    void findDelivery_hubManagerOutsideScope_throwsForbidden() {
        UUID deliveryId = UUID.randomUUID();
        UUID hubManagerId = UUID.randomUUID();
        Delivery delivery = Delivery.builder()
                .deliveryId(deliveryId)
                .orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID())
                .destHubId(UUID.randomUUID())
                .deliveryAddress("주소")
                .recipientName("수령인")
                .build();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(userClient.getUser(hubManagerId)).thenReturn(ApiResponse.success(
                UserInfoResponse.builder().userId(hubManagerId).role(UserRole.HUB_MANAGER).hubId(UUID.randomUUID()).build()
        ));

        assertThatThrownBy(() -> deliveryService.findDelivery(deliveryId, hubManagerId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryErrorCode.FORBIDDEN_DELIVERY_SCOPE));
    }

    @Test
    void updateDelivery_supplierManager_throwsAccessDenied() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = Delivery.builder().deliveryId(deliveryId).orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .deliveryAddress("주소").recipientName("수령인").build();
        when(deliveryRepository.findByDeliveryIdAndDeletedAtIsNull(deliveryId)).thenReturn(Optional.of(delivery));

        DeliveryUpdateRequestDto request = DeliveryUpdateRequestDto.builder().status("DEST_HUB_ARRIVED").build();

        assertThatThrownBy(() -> deliveryService.updateDelivery(deliveryId, request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void deleteDelivery_deliveryManager_throwsAccessDenied() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = Delivery.builder().deliveryId(deliveryId).orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .deliveryAddress("주소").recipientName("수령인").build();
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.deleteDelivery(deliveryId, UUID.randomUUID(), UserRole.DELIVERY_MANAGER, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void deleteDelivery_internalOrderServiceCall_cascadesRouteSoftDelete() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = Delivery.builder().deliveryId(deliveryId).orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .deliveryAddress("주소").recipientName("수령인").build();
        DeliveryRoute route = DeliveryRoute.builder()
                .Id(UUID.randomUUID()).delivery(delivery).sequence(0)
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID()).build();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryOrderBySequenceAsc(delivery)).thenReturn(List.of(route));

        // DELIVERY_MANAGER는 D5 권한이 없지만 내부(order-service) 호출이면 role 검사를 건너뛴다.
        deliveryService.deleteDelivery(deliveryId, UUID.randomUUID(), UserRole.DELIVERY_MANAGER, "order-service");

        assertThat(delivery.isDeleted()).isTrue();
        assertThat(route.isDeleted()).isTrue();
    }

    @Test
    void assignNextManager_cyclesThroughGroupInSequenceOrder() {
        UUID hubId = UUID.randomUUID();
        UUID managerA = UUID.randomUUID();
        UUID managerB = UUID.randomUUID();
        UUID managerC = UUID.randomUUID();

        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(companyManager(managerA, hubId, 0), companyManager(managerB, hubId, 1), companyManager(managerC, hubId, 2)));

        DeliveryManagerCursor cursor = DeliveryManagerCursor.builder()
                .type(DeliveryManagerType.COMPANY).hubId(hubId).lastAssignedManagerId(managerB)
                .build();
        when(deliveryManagerCursorRepository.findLastAssignedManagerIdByTypeAndHubIdAndDeletedAtIsNull(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(Optional.of(cursor));

        UUID next = deliveryService.assignNextManager(DeliveryManagerType.COMPANY, hubId);

        assertThat(next).isEqualTo(managerC);
        assertThat(cursor.getLastAssignedManagerId()).isEqualTo(managerC);
    }

    @Test
    void searchDelivery_master_returnsPagedSummaries() {
        Delivery delivery = Delivery.builder()
                .deliveryId(UUID.randomUUID()).orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .deliveryAddress("주소").recipientName("김말숙").build();
        Pageable pageable = PageRequest.of(0, 10);

        when(deliveryRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(delivery), pageable, 1));

        ApiResponse<PageResponse<DeliverySummaryResponseDto>> response =
                deliveryService.searchDelivery(null, null, null, pageable, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().content()).hasSize(1);
        assertThat(response.getData().content().get(0).getDeliveryId()).isEqualTo(delivery.getDeliveryId());
    }

    @Test
    void searchDelivery_hubManager_scopesByFeignHubId() {
        UUID userId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        when(userClient.getUser(userId)).thenReturn(ApiResponse.success(
                UserInfoResponse.builder().userId(userId).role(UserRole.HUB_MANAGER).hubId(hubId).build()
        ));
        when(deliveryRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        deliveryService.searchDelivery(null, null, null, PageRequest.of(0, 10), userId, UserRole.HUB_MANAGER);

        ArgumentCaptor<UUID> scopeHubIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(deliveryRepository).search(any(), any(), any(), scopeHubIdCaptor.capture(), any(), any(), any());
        assertThat(scopeHubIdCaptor.getValue()).isEqualTo(hubId);
    }

    @Test
    void searchDelivery_deliveryManager_scopesByOwnRoutes() {
        UUID userId = UUID.randomUUID();
        List<UUID> routeDeliveryIds = List.of(UUID.randomUUID());
        when(deliveryRouteRepository.findDeliveryIdByHubDeliveryManagerId(userId)).thenReturn(routeDeliveryIds);
        when(deliveryRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        deliveryService.searchDelivery(null, null, null, PageRequest.of(0, 10), userId, UserRole.DELIVERY_MANAGER);

        ArgumentCaptor<UUID> scopeManagerIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(deliveryRepository).search(any(), any(), any(), any(), scopeManagerIdCaptor.capture(), eq(routeDeliveryIds), any());
        assertThat(scopeManagerIdCaptor.getValue()).isEqualTo(userId);
    }

    @Test
    void searchDelivery_supplierManager_notYetSupported() {
        assertThatThrownBy(() -> deliveryService.searchDelivery(
                null, null, null, PageRequest.of(0, 10), UUID.randomUUID(), UserRole.SUPPLIER_MANAGER
        )).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void searchDelivery_invalidStatus_throwsInvalidInput() {
        assertThatThrownBy(() -> deliveryService.searchDelivery(
                "NOT_A_STATUS", null, null, PageRequest.of(0, 10), UUID.randomUUID(), UserRole.MASTER
        )).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void findDeliveryRoutes_returnsRoutesInSequenceOrder() {
        UUID deliveryId = UUID.randomUUID();
        Delivery delivery = Delivery.builder().deliveryId(deliveryId).orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .deliveryAddress("주소").recipientName("수령인").build();
        DeliveryRoute route0 = route(delivery, 0);
        DeliveryRoute route1 = route(delivery, 1);

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
        when(deliveryRouteRepository.findAllByDeliveryOrderBySequenceAsc(delivery)).thenReturn(List.of(route0, route1));

        ApiResponse<List<DeliveryRouteSearchResponseDto>> response =
                deliveryService.findDeliveryRoutes(deliveryId, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.getData()).extracting(DeliveryRouteSearchResponseDto::getSequence).containsExactly(0, 1);
    }

    @Test
    void findDeliveryRoutes_notFound_throws() {
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.findDeliveryRoutes(deliveryId, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryErrorCode.DELIVERY_NOT_FOUND));
    }

    @Test
    void updateDeliveryRoute_masterSuccess_updatesStatus() {
        UUID routeId = UUID.randomUUID();
        DeliveryRoute route = route(null, 0);
        when(deliveryRouteRepository.findById(routeId)).thenReturn(Optional.of(route));

        DeliveryRouteUpdateRequestDto request = DeliveryRouteUpdateRequestDto.builder().status("DEST_HUB_ARRIVED").build();

        deliveryService.updateDeliveryRoute(routeId, request, UUID.randomUUID(), UserRole.MASTER);

        assertThat(route.getStatus()).isEqualTo(DeliveryRouteStatusEnum.DEST_HUB_ARRIVED);
    }

    @Test
    void updateDeliveryRoute_notFound_throws() {
        UUID routeId = UUID.randomUUID();
        when(deliveryRouteRepository.findById(routeId)).thenReturn(Optional.empty());

        DeliveryRouteUpdateRequestDto request = DeliveryRouteUpdateRequestDto.builder().build();

        assertThatThrownBy(() -> deliveryService.updateDeliveryRoute(routeId, request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryErrorCode.DELIVERY_ROUTE_NOT_FOUND));
    }

    @Test
    void updateDeliveryRoute_supplierManager_throwsAccessDenied() {
        UUID routeId = UUID.randomUUID();
        when(deliveryRouteRepository.findById(routeId)).thenReturn(Optional.of(route(null, 0)));

        DeliveryRouteUpdateRequestDto request = DeliveryRouteUpdateRequestDto.builder().build();

        assertThatThrownBy(() -> deliveryService.updateDeliveryRoute(routeId, request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void updateDeliveryRoute_deliveryManagerNotAssigned_throwsForbidden() {
        UUID routeId = UUID.randomUUID();
        DeliveryRoute route = route(null, 0);
        when(deliveryRouteRepository.findById(routeId)).thenReturn(Optional.of(route));

        DeliveryRouteUpdateRequestDto request = DeliveryRouteUpdateRequestDto.builder().build();

        assertThatThrownBy(() -> deliveryService.updateDeliveryRoute(routeId, request, UUID.randomUUID(), UserRole.DELIVERY_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryErrorCode.FORBIDDEN_DELIVERY_SCOPE));
    }

    @Test
    void updateDeliveryRoute_assignedDeliveryManager_success() {
        UUID routeId = UUID.randomUUID();
        UUID hubDeliveryManagerId = UUID.randomUUID();
        DeliveryRoute route = DeliveryRoute.builder()
                .Id(routeId).sequence(0)
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .hubDeliveryManagerId(hubDeliveryManagerId)
                .build();
        when(deliveryRouteRepository.findById(routeId)).thenReturn(Optional.of(route));

        DeliveryRouteUpdateRequestDto request = DeliveryRouteUpdateRequestDto.builder().status("OUT_FOR_DELIVERY").build();

        deliveryService.updateDeliveryRoute(routeId, request, hubDeliveryManagerId, UserRole.DELIVERY_MANAGER);

        assertThat(route.getStatus()).isEqualTo(DeliveryRouteStatusEnum.OUT_FOR_DELIVERY);
    }

    private DeliveryRoute route(Delivery delivery, int sequence) {
        return DeliveryRoute.builder()
                .Id(UUID.randomUUID()).delivery(delivery).sequence(sequence)
                .originHubId(UUID.randomUUID()).destHubId(UUID.randomUUID())
                .build();
    }

    private DeliveryCreateRequestDto createRequest(UUID destHubId) {
        return DeliveryCreateRequestDto.builder()
                .orderId(UUID.randomUUID())
                .originHubId(UUID.randomUUID())
                .destHubId(destHubId)
                .deliveryAddress("부산시 사하구 낙동대로 1번길 1")
                .recipientName("김말숙")
                .recipientSlackId("U0123ABC")
                .build();
    }

    private DeliveryManager companyManager(UUID userId, UUID hubId, int sequence) {
        return DeliveryManager.builder()
                .userId(userId).type(DeliveryManagerType.COMPANY).hubId(hubId).sequence(sequence)
                .build();
    }

    private Delivery argThatCompanyManagerIs(UUID managerId) {
        return argThat(d -> Objects.equals(d.getCompanyDeliveryManagerId(), managerId));
    }
}
