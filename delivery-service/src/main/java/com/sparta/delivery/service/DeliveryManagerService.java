package com.sparta.delivery.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.domain.dto.request.DeliveryManagerCreateRequestDto;
import com.sparta.delivery.domain.dto.request.DeliveryManagerUpdateRequestDto;
import com.sparta.delivery.domain.dto.response.DeliveryManagerResponseDto;
import com.sparta.delivery.domain.dto.response.DeliveryManagerSearchResponseDto;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import com.sparta.delivery.repository.DeliveryManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 배송 담당자(DM1~DM5) 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class DeliveryManagerService {

    private final DeliveryManagerRepository deliveryManagerRepository;
    // TODO: UserClient(role=DELIVERY_MANAGER 사용자인지/HUB_MANAGER 담당허브 확인), HubClient(허브 존재확인)

    /**
     * DM1. 배송 담당자 생성.
     *  1) 인가: MASTER 전체 / HUB_MANAGER는 자기 담당 허브 소속만
     *  2) 검증: 대상 userId가 role=DELIVERY_MANAGER 사용자인지(Feign),
     *     type=COMPANY면 hubId 필수+허브 존재확인 / type=HUB면 hubId는 null
     *  3) 중복 등록(user_id) 방어 → 있으면 CONFLICT(공통코드 없으면 INVALID_INPUT+메시지)
     *  4) sequence = 그룹(HUB 전체 / COMPANY 허브별) max+1 로 자동 부여 후 저장
     */
    @Transactional
    public ApiResponse<DeliveryManagerResponseDto> createDeliveryManager(
            DeliveryManagerCreateRequestDto request, UUID userId, UserRole role) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * DM2. 배송 담당자 단건 조회.
     *  1) 조회(없으면 ENTITY_NOT_FOUND)
     *  2) 인가: MASTER / HUB_MANAGER(담당 허브 소속) / DELIVERY_MANAGER(본인=targetUserId==userId) /
     *     SUPPLIER_MANAGER 불가
     */
    @Transactional(readOnly = true)
    public ApiResponse<DeliveryManagerResponseDto> findDeliveryManager(
            UUID targetUserId, UUID userId, UserRole role) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * DM3. 배송 담당자 목록/검색.
     *  1) size 10/30/50 보정
     *  2) role별 필터: MASTER=전체 / HUB_MANAGER=담당 허브 소속만 / DELIVERY_MANAGER=본인만 / SUPPLIER 불가
     *  3) type/hubId 조건 + 페이징 조회
     */
    @Transactional(readOnly = true)
    public ApiResponse<DeliveryManagerSearchResponseDto> searchDeliveryManagers(
            DeliveryManagerType type, UUID hubId, Pageable pageable, UUID userId, UserRole role) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * DM4. 배송 담당자 수정(타입/소속허브/순번).
     *  1) 인가: MASTER / 담당 허브 HUB_MANAGER
     *  2) type=COMPANY 유지·변경 시 hubId 존재 허브 확인 / HUB로 변경 시 hubId=null 처리
     *  3) sequence 재지정 시 그룹 내 유일성 고려(부분 수정)
     */
    @Transactional
    public ApiResponse<DeliveryManagerResponseDto> updateDeliveryManager(
            UUID targetUserId, DeliveryManagerUpdateRequestDto request, UUID userId, UserRole role) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * DM5. 배송 담당자 삭제 (Soft Delete).
     *  1) 인가: MASTER / 담당 허브 HUB_MANAGER
     *  2) softDelete(userId). 남은 담당자의 sequence는 재배열하지 않음(라운드로빈 대상에서만 제외).
     */
    @Transactional
    public ApiResponse<Void> deleteDeliveryManager(UUID targetUserId, UUID userId, UserRole role) {
        throw new UnsupportedOperationException("TODO");
    }
}
