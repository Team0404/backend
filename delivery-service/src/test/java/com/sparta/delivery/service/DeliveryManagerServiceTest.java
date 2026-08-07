package com.sparta.delivery.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.client.HubClient;
import com.sparta.delivery.client.UserClient;
import com.sparta.delivery.client.dto.HubResponse;
import com.sparta.delivery.domain.dto.request.DeliveryManagerCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryManagerUpdateRequestDto;
import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.exception.DeliveryManagerErrorCode;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryManagerServiceTest {

    private DeliveryManagerRepository deliveryManagerRepository;
    private UserClient userClient;
    private HubClient hubClient;
    private DeliveryManagerService deliveryManagerService;

    @BeforeEach
    void setUp() {
        deliveryManagerRepository = mock(DeliveryManagerRepository.class);
        userClient = mock(UserClient.class);
        hubClient = mock(HubClient.class);
        deliveryManagerService = new DeliveryManagerService(deliveryManagerRepository, userClient, hubClient);
    }

    // ── DM1 생성 ────────────────────────────────────────────────

    @Test
    void createDeliveryManager_masterSuccess_companyType() {
        UUID targetUserId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(targetUserId, DeliveryManagerType.COMPANY, hubId);

        mockHubExists(hubId, true);
        mockUserInfo(targetUserId, UserRole.DELIVERY_MANAGER, null);
        when(deliveryManagerRepository.findFirstByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceDesc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(Optional.empty());
        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getSequence()).isZero();
        assertThat(response.getData().getHubId()).isEqualTo(hubId);
    }

    @Test
    void createDeliveryManager_hubType_hubIdForcedNullRegardlessOfRequest() {
        UUID targetUserId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(targetUserId, DeliveryManagerType.HUB, UUID.randomUUID());

        mockUserInfo(targetUserId, UserRole.DELIVERY_MANAGER, null);
        when(deliveryManagerRepository.findFirstByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceDesc(eq(DeliveryManagerType.HUB), any()))
                .thenReturn(Optional.empty());
        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.getData().getHubId()).isNull();
        verify(hubClient, never()).findHub(any());
    }

    @Test
    void createDeliveryManager_companyMissingHubId_throwsInvalidInput() {
        DeliveryManagerCreateRequestDto request = createRequest(UUID.randomUUID(), DeliveryManagerType.COMPANY, null);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void createDeliveryManager_nonexistentHub_throwsInvalidHubId() {
        UUID hubId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(UUID.randomUUID(), DeliveryManagerType.COMPANY, hubId);
        mockHubExists(hubId, false);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.INVALID_HUB_ID));
    }

    @Test
    void createDeliveryManager_targetNotDeliveryManagerRole_throws() {
        UUID targetUserId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(targetUserId, DeliveryManagerType.COMPANY, hubId);
        mockHubExists(hubId, true);
        mockUserInfo(targetUserId, UserRole.SUPPLIER_MANAGER, null);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.NOT_DELIVERY_MANAGER_ROLE));
    }

    @Test
    void createDeliveryManager_duplicateUserId_throwsAlreadyExists() {
        UUID targetUserId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(targetUserId, DeliveryManagerType.COMPANY, hubId);
        mockHubExists(hubId, true);
        mockUserInfo(targetUserId, UserRole.DELIVERY_MANAGER, null);
        when(deliveryManagerRepository.findFirstByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(deliveryManagerRepository.saveAndFlush(any(DeliveryManager.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.DELIVERY_MANAGER_ALREADY_EXISTS));
    }

    @Test
    void createDeliveryManager_hubManagerForDifferentHub_throwsForbidden() {
        UUID requesterId = UUID.randomUUID();
        UUID ownHubId = UUID.randomUUID();
        UUID targetHubId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(UUID.randomUUID(), DeliveryManagerType.COMPANY, targetHubId);
        mockHubExists(targetHubId, true);
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, ownHubId);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, requesterId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE));
    }

    @Test
    void createDeliveryManager_hubManagerForHubType_throwsForbidden() {
        UUID requesterId = UUID.randomUUID();
        DeliveryManagerCreateRequestDto request = createRequest(UUID.randomUUID(), DeliveryManagerType.HUB, null);
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, UUID.randomUUID());

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, requesterId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE));
    }

    @Test
    void createDeliveryManager_supplierManager_throwsAccessDenied() {
        DeliveryManagerCreateRequestDto request = createRequest(UUID.randomUUID(), DeliveryManagerType.COMPANY, UUID.randomUUID());
        mockHubExists(request.getHubId(), true);

        assertThatThrownBy(() -> deliveryManagerService.createDeliveryManager(request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    // ── DM5 삭제 ────────────────────────────────────────────────

    @Test
    void deleteDeliveryManager_masterSuccess_softDeletes() {
        UUID targetUserId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, UUID.randomUUID(), 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));

        deliveryManagerService.deleteDeliveryManager(targetUserId, UUID.randomUUID(), UserRole.MASTER);

        assertThat(dm.isDeleted()).isTrue();
    }

    @Test
    void deleteDeliveryManager_notFound_throws() {
        UUID targetUserId = UUID.randomUUID();
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryManagerService.deleteDeliveryManager(targetUserId, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));
    }

    @Test
    void deleteDeliveryManager_hubManagerOtherHub_throwsForbidden() {
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, UUID.randomUUID(), 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, UUID.randomUUID());

        assertThatThrownBy(() -> deliveryManagerService.deleteDeliveryManager(targetUserId, requesterId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE));
        assertThat(dm.isDeleted()).isFalse();
    }

    // ── DM4 수정 ────────────────────────────────────────────────

    @Test
    void updateDeliveryManager_notFound_throws() {
        UUID targetUserId = UUID.randomUUID();
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.empty());

        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().build();

        assertThatThrownBy(() -> deliveryManagerService.updateDeliveryManager(targetUserId, request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));
    }

    @Test
    void updateDeliveryManager_partialUpdate_keepsExistingTypeAndHub() {
        UUID targetUserId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, hubId, 3);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(dm));
        mockHubExists(hubId, true);

        // type/hubId 미지정, sequence만 변경 (자기 자신의 기존 순번이 아닌 새 값)
        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().sequence(5).build();

        var response = deliveryManagerService.updateDeliveryManager(targetUserId, request, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.getData().getType()).isEqualTo(DeliveryManagerType.COMPANY);
        assertThat(response.getData().getHubId()).isEqualTo(hubId);
        assertThat(response.getData().getSequence()).isEqualTo(5);
    }

    @Test
    void updateDeliveryManager_resendingOwnSequence_isNotTreatedAsDuplicate() {
        UUID targetUserId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, hubId, 3);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(dm));
        mockHubExists(hubId, true);

        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().sequence(3).build();

        var response = deliveryManagerService.updateDeliveryManager(targetUserId, request, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.getData().getSequence()).isEqualTo(3);
    }

    @Test
    void updateDeliveryManager_sequenceTakenByAnother_throwsDuplicate() {
        UUID targetUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, hubId, 3);
        DeliveryManager other = deliveryManager(otherUserId, DeliveryManagerType.COMPANY, hubId, 5);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, hubId))
                .thenReturn(List.of(dm, other));
        mockHubExists(hubId, true);

        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().sequence(5).build();

        assertThatThrownBy(() -> deliveryManagerService.updateDeliveryManager(targetUserId, request, UUID.randomUUID(), UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.DUPLICATE_SEQUENCE));
    }

    @Test
    void updateDeliveryManager_groupChangedWithoutSequence_autoAssignsNextInNewGroup() {
        UUID targetUserId = UUID.randomUUID();
        UUID oldHubId = UUID.randomUUID();
        UUID newHubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, oldHubId, 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        mockHubExists(newHubId, true);
        when(deliveryManagerRepository.findFirstByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceDesc(DeliveryManagerType.COMPANY, newHubId))
                .thenReturn(Optional.of(deliveryManager(UUID.randomUUID(), DeliveryManagerType.COMPANY, newHubId, 2)));

        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().hubId(newHubId).build();

        var response = deliveryManagerService.updateDeliveryManager(targetUserId, request, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.getData().getHubId()).isEqualTo(newHubId);
        assertThat(response.getData().getSequence()).isEqualTo(3);
    }

    @Test
    void updateDeliveryManager_hubManager_deniedWhenCurrentGroupNotOwnHub() {
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID currentHubId = UUID.randomUUID();
        UUID ownHubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, currentHubId, 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, ownHubId);

        // 요청 자체는 자기 허브(ownHubId)로 옮기려는 것이지만, 대상이 원래 자기 담당 허브 소속이 아님
        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().hubId(ownHubId).build();
        mockHubExists(ownHubId, true);

        assertThatThrownBy(() -> deliveryManagerService.updateDeliveryManager(targetUserId, request, requesterId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE));
    }

    @Test
    void updateDeliveryManager_hubManager_deniedWhenTargetGroupNotOwnHub() {
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID ownHubId = UUID.randomUUID();
        UUID otherHubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, ownHubId, 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, ownHubId);
        mockHubExists(otherHubId, true);

        // 본인 담당 허브 소속 담당자를, 자기가 관리하지 않는 다른 허브로 옮기려는 시도
        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().hubId(otherHubId).build();

        assertThatThrownBy(() -> deliveryManagerService.updateDeliveryManager(targetUserId, request, requesterId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE));
    }

    @Test
    void updateDeliveryManager_hubManager_successWithinOwnHub() {
        UUID requesterId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID ownHubId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, ownHubId, 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));
        when(deliveryManagerRepository.findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType.COMPANY, ownHubId))
                .thenReturn(List.of(dm));
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, ownHubId);
        mockHubExists(ownHubId, true);

        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().sequence(1).build();

        var response = deliveryManagerService.updateDeliveryManager(targetUserId, request, requesterId, UserRole.HUB_MANAGER);

        assertThat(response.getData().getSequence()).isEqualTo(1);
    }

    @Test
    void updateDeliveryManager_supplierManager_throwsAccessDenied() {
        UUID targetUserId = UUID.randomUUID();
        DeliveryManager dm = deliveryManager(targetUserId, DeliveryManagerType.COMPANY, UUID.randomUUID(), 0);
        when(deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(Optional.of(dm));

        DeliveryManagerUpdateRequestDto request = DeliveryManagerUpdateRequestDto.builder().sequence(1).build();

        assertThatThrownBy(() -> deliveryManagerService.updateDeliveryManager(targetUserId, request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    // ── DM3 검색/권한 ────────────────────────────────────────────

    @Test
    void searchDeliveryManagers_master_noScopeRestriction() {
        Pageable pageable = PageRequest.of(0, 10);
        when(deliveryManagerRepository.search(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(deliveryManager(UUID.randomUUID(), DeliveryManagerType.HUB, null, 0))));

        var response = deliveryManagerService.searchDeliveryManagers(null, null, pageable, UUID.randomUUID(), UserRole.MASTER);

        assertThat(response.getData().getContent()).hasSize(1);
        verify(userClient, never()).getUser(any());
    }

    @Test
    void searchDeliveryManagers_hubManager_autoScopesToOwnHubWhenOmitted() {
        UUID requesterId = UUID.randomUUID();
        UUID ownHubId = UUID.randomUUID();
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, ownHubId);
        when(deliveryManagerRepository.search(any(), eq(ownHubId), any()))
                .thenReturn(new PageImpl<>(List.of()));

        deliveryManagerService.searchDeliveryManagers(null, null, PageRequest.of(0, 10), requesterId, UserRole.HUB_MANAGER);

        verify(deliveryManagerRepository).search(any(), eq(ownHubId), any());
    }

    @Test
    void searchDeliveryManagers_hubManager_deniedForOtherHub() {
        UUID requesterId = UUID.randomUUID();
        mockUserInfo(requesterId, UserRole.HUB_MANAGER, UUID.randomUUID());

        assertThatThrownBy(() -> deliveryManagerService.searchDeliveryManagers(
                null, UUID.randomUUID(), PageRequest.of(0, 10), requesterId, UserRole.HUB_MANAGER
        )).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE));
    }

    @Test
    void searchDeliveryManagers_deliveryManager_onlyOwnRecords() {
        UUID requesterId = UUID.randomUUID();
        when(deliveryManagerRepository.findAllByUserIdAndDeletedAtIsNull(eq(requesterId), any()))
                .thenReturn(new PageImpl<>(List.of(deliveryManager(requesterId, DeliveryManagerType.COMPANY, UUID.randomUUID(), 0))));

        var response = deliveryManagerService.searchDeliveryManagers(
                null, null, PageRequest.of(0, 10), requesterId, UserRole.DELIVERY_MANAGER
        );

        assertThat(response.getData().getContent()).extracting(dto -> dto.getUserId()).containsExactly(requesterId);
        verify(deliveryManagerRepository, never()).search(any(), any(), any());
    }

    @Test
    void searchDeliveryManagers_supplierManager_throwsAccessDenied() {
        assertThatThrownBy(() -> deliveryManagerService.searchDeliveryManagers(
                null, null, PageRequest.of(0, 10), UUID.randomUUID(), UserRole.SUPPLIER_MANAGER
        )).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    // ── 헬퍼 ────────────────────────────────────────────────

    private DeliveryManagerCreateRequestDto createRequest(UUID userId, DeliveryManagerType type, UUID hubId) {
        return DeliveryManagerCreateRequestDto.builder()
                .userId(userId)
                .type(type)
                .hubId(hubId)
                .build();
    }

    private DeliveryManager deliveryManager(UUID userId, DeliveryManagerType type, UUID hubId, int sequence) {
        return DeliveryManager.builder()
                .userId(userId).type(type).hubId(hubId).sequence(sequence)
                .build();
    }

    private void mockUserInfo(UUID userId, UserRole role, UUID hubId) {
        when(userClient.getUser(userId)).thenReturn(ApiResponse.success(
                UserInfoResponse.builder().userId(userId).role(role).hubId(hubId).build()
        ));
    }

    private void mockHubExists(UUID hubId, boolean exists) {
        ApiResponse<HubResponse> body = exists
                ? ApiResponse.success(new HubResponse())
                : castToHubResponse(ApiResponse.error(ErrorCode.ENTITY_NOT_FOUND));
        when(hubClient.findHub(hubId)).thenReturn(ResponseEntity.ok(body));
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<HubResponse> castToHubResponse(ApiResponse<?> response) {
        return (ApiResponse<HubResponse>) response;
    }
}
