package com.sparta.delivery.repository;

import com.sparta.delivery.domain.entity.DeliveryManager;
import com.sparta.delivery.domain.entity.DeliveryManagerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository extends JpaRepository<DeliveryManager, UUID> {
    Optional<DeliveryManager> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<DeliveryManager> findAllByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceAsc(DeliveryManagerType type, UUID hubId);

    /**
     * 그룹(type + hubId, HUB는 hubId=null)에서 가장 큰 sequence를 가진 담당자.
     * null 파라미터는 Spring Data가 derived query에서 자동으로 IS NULL로 변환해주므로
     * HUB 타입(hubId=null) 조회에도 안전하게 사용 가능.
     */
    Optional<DeliveryManager> findFirstByTypeAndHubIdAndDeletedAtIsNullOrderBySequenceDesc(DeliveryManagerType type, UUID hubId);

    Page<DeliveryManager> findAllByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    /**
     * DM3 검색. type/hubId는 옵셔널 필터(null이면 조건 미적용).
     * 각 조건은 반드시 괄호로 묶어야 한다 — JPQL도 AND가 OR보다 우선순위가 높아서
     * 괄호가 없으면 deletedAt 조건이 OR 뒤쪽 항과 분리되어 삭제된 행이 조회된다.
     */
    @Query("""
            SELECT dm FROM DeliveryManager dm
            WHERE dm.deletedAt IS NULL
              AND (:type IS NULL OR dm.type = :type)
              AND (:hubId IS NULL OR dm.hubId = :hubId)
            """)
    Page<DeliveryManager> search(@Param("type") DeliveryManagerType type,
                                 @Param("hubId") UUID hubId,
                                 Pageable pageable);
}
