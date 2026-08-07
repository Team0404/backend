package com.sparta.delivery.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.util.PageableUtil;
import com.sparta.delivery.client.HubClient;
import com.sparta.delivery.client.UserClient;
import com.sparta.delivery.client.dto.HubResponse;
import com.sparta.delivery.domain.dto.request.DeliveryManagerCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryManagerUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.DeliveryManagerResponseDto;
import com.sparta.delivery.domain.dto.response.DeliveryManagerSearchResponseDto;
import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.exception.DeliveryManagerErrorCode;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 배송 담당자(DM1~DM5) 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class DeliveryManagerService {

    private final DeliveryManagerRepository deliveryManagerRepository;
    private final UserClient userClient;
    private final HubClient hubClient;

    @Transactional
    public ApiResponse<DeliveryManagerResponseDto> createDeliveryManager(
            DeliveryManagerCreateRequestDto request, UUID userId, UserRole role) {
        UUID hubId = validateHubId(request.getHubId(), request.getType());

        authorizeManagerWrite(userId, role, request.getType(), hubId);

        UserInfoResponse userInfo = getUserInfoFeign(request.getUserId());
        if (!UserRole.DELIVERY_MANAGER.equals(userInfo.getRole())) {
            throw new BusinessException(DeliveryManagerErrorCode.NOT_DELIVERY_MANAGER_ROLE);
        }

        DeliveryManager dm = DeliveryManager.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .hubId(hubId)
                .sequence(nextSequence(request.getType(), hubId))
                .build();

        DeliveryManager savedDm;
        try {
            savedDm = deliveryManagerRepository.saveAndFlush(dm);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(DeliveryManagerErrorCode.DELIVERY_MANAGER_ALREADY_EXISTS);
        }

        return ApiResponse.success(DeliveryManagerResponseDto.fromDM(savedDm));
    }

    @Transactional(readOnly = true)
    public ApiResponse<DeliveryManagerResponseDto> findDeliveryManager(
            UUID targetUserId, UUID userId, UserRole role) {
        DeliveryManager dm = deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new BusinessException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        authorizeManagerRead(dm, userId, role);

        return ApiResponse.success(DeliveryManagerResponseDto.fromDM(dm));
    }

    @Transactional(readOnly = true)
    public ApiResponse<DeliveryManagerSearchResponseDto> searchDeliveryManagers(
            DeliveryManagerType type, UUID hubId, Pageable pageable, UUID userId, UserRole role) {
        Pageable normalized = PageableUtil.normalize(pageable);

        Page<DeliveryManager> result = switch (role) {
            case MASTER -> deliveryManagerRepository.search(type, hubId, normalized);
            case HUB_MANAGER -> {
                UUID managerHubId = getUserInfoFeign(userId).getHubId();
                if (managerHubId == null) {
                    throw new BusinessException(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE);
                }
                // hubId 미지정이면 본인 허브로 자동 한정, 지정했다면 본인 허브여야 한다.
                if (hubId != null && !managerHubId.equals(hubId)) {
                    throw new BusinessException(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE);
                }
                yield deliveryManagerRepository.search(type, managerHubId, normalized);
            }
            case DELIVERY_MANAGER -> deliveryManagerRepository.findAllByUserIdAndDeletedAtIsNull(userId, normalized);
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        };

        return ApiResponse.success(DeliveryManagerSearchResponseDto.builder()
                .content(result.getContent().stream().map(DeliveryManagerResponseDto::fromDM).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build());
    }

    @Transactional
    public ApiResponse<DeliveryManagerResponseDto> updateDeliveryManager(
            UUID targetUserId, DeliveryManagerUpdateRequestDto request, UUID userId, UserRole role) {
        DeliveryManager dm = deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new BusinessException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        // 현재 소속 그룹 권한을 허브 존재 검증(Feign)보다 먼저 확인한다.
        authorizeManagerWrite(userId, role, dm.getType(), dm.getHubId());

        DeliveryManagerType newType = request.getType() != null ? request.getType() : dm.getType();
        UUID requestedHubId = request.getHubId() != null ? request.getHubId() : dm.getHubId();
        UUID newHubId = validateHubId(requestedHubId, newType);

        // 이동하려는 새 그룹에 대한 권한도 확인한다.
        // 한쪽만 보면 A허브 관리자가 B허브 소속 담당자를 자기 허브로 데려올 수 있다.
        authorizeManagerWrite(userId, role, newType, newHubId);

        boolean groupChanged = newType != dm.getType() || !Objects.equals(newHubId, dm.getHubId());

        Integer newSequence;
        if (request.getSequence() != null) {
            newSequence = request.getSequence();
            validateSequenceAvailable(newType, newHubId, newSequence, targetUserId);
        } else if (groupChanged) {
            newSequence = nextSequence(newType, newHubId);
        } else {
            newSequence = dm.getSequence();
        }

        dm.update(newType, newHubId, newSequence);

        return ApiResponse.success(DeliveryManagerResponseDto.fromDM(dm));
    }

    @Transactional
    public ApiResponse<Void> deleteDeliveryManager(UUID targetUserId, UUID userId, UserRole role) {
        DeliveryManager dm = deliveryManagerRepository.findByUserIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new BusinessException(DeliveryManagerErrorCode.DELIVERY_MANAGER_NOT_FOUND));

        authorizeManagerWrite(userId, role, dm.getType(), dm.getHubId());

        // 남은 담당자의 sequence는 재배열하지 않는다. deletedAt 필터로 라운드로빈 대상에서만 빠진다.
        dm.softDelete(userId);

        return ApiResponse.success(null);
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    /**
     * 허브 생성, 수정 전에 허브가 DB에 있는지 확인
     * type == hub면 상관 없음.
     */
    private UUID validateHubId(UUID targetHubId, DeliveryManagerType type) {
        UUID hubId = null;
        if (DeliveryManagerType.COMPANY.equals(type)) {
            if (targetHubId == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            if (!existHubFeign(targetHubId)) {
                throw new BusinessException(DeliveryManagerErrorCode.INVALID_HUB_ID);
            }
            hubId = targetHubId;
        }

        return hubId;
    }

    /**
     * 그룹(HUB=전체 풀 / COMPANY=해당 허브)에서 마지막 순번 다음 값.
     * 그룹이 비어 있으면 0부터 시작한다.
     */
    private int nextSequence(DeliveryManagerType type, UUID hubId) {
        return deliveryManagerRepository
                .findFirstByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceDesc(type, hubId)
                .map(dm -> dm.getSequence() + 1)
                .orElse(0);
    }

    /**
     * 그룹 내 sequence 중복 검사. 본인(excludeUserId)은 제외해야
     * 자기 순번을 그대로 다시 보내는 요청이 충돌로 거부되지 않는다.
     */
    private void validateSequenceAvailable(DeliveryManagerType type, UUID hubId, Integer sequence, UUID excludeUserId) {
        boolean taken = deliveryManagerRepository
                .findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(type, hubId).stream()
                .anyMatch(other -> !other.getUserId().equals(excludeUserId) && sequence.equals(other.getSequence()));

        if (taken) {
            throw new BusinessException(DeliveryManagerErrorCode.DUPLICATE_SEQUENCE);
        }
    }

    private void authorizeManagerWrite(UUID userId, UserRole role, DeliveryManagerType type, UUID hubId) {
        switch (role) {
            case MASTER -> {
            }
            case HUB_MANAGER -> {
                UserInfoResponse userInfo = getUserInfoFeign(userId);
                UUID managerHubId = userInfo.getHubId();
                if (type != DeliveryManagerType.COMPANY || managerHubId == null || !managerHubId.equals(hubId)) {
                    throw new BusinessException(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE);
                }
            }
            default -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void authorizeManagerRead(DeliveryManager dm, UUID userId, UserRole role) {
        switch (role) {
            case MASTER -> {
            }
            case HUB_MANAGER -> {
                UserInfoResponse userInfo = getUserInfoFeign(userId);
                UUID managerHubId = userInfo.getHubId();
                if (managerHubId == null || !managerHubId.equals(dm.getHubId())) {
                    throw new BusinessException(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE);
                }
            }
            case DELIVERY_MANAGER -> {
                if (!userId.equals(dm.getUserId())) {
                    throw new BusinessException(DeliveryManagerErrorCode.FORBIDDEN_DELIVERY_MANAGER_SCOPE);
                }
            }
            case SUPPLIER_MANAGER -> throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private UserInfoResponse getUserInfoFeign(UUID userId) {
        ApiResponse<UserInfoResponse> userResponse = userClient.getUser(userId);
        if (!userResponse.isSuccess()) {
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "DeliveryManager -> User 조회 실패");
        }
        return userResponse.getData();
    }

    private boolean existHubFeign(UUID hubId) {
        ResponseEntity<ApiResponse<HubResponse>> hubResponse = hubClient.findHub(hubId);
        ApiResponse<HubResponse> body = Optional.ofNullable(hubResponse.getBody())
                .orElseThrow(() -> new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR));

        return body.isSuccess();
    }

    private HubResponse getHubFeign(UUID hubId) {
        ResponseEntity<ApiResponse<HubResponse>> hubResponse = hubClient.findHub(hubId);
        ApiResponse<HubResponse> body = Optional.ofNullable(hubResponse.getBody())
                .orElseThrow(() -> new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR));

        if (!body.isSuccess()) {
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR, "DeliveryManager -> Hub 조회 실패");
        }
        return body.getData();
    }
}
