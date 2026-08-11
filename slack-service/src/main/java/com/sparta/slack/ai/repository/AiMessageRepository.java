package com.sparta.slack.ai.repository;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import com.sparta.slack.ai.domain.entity.AiMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    /** 모든 조회는 Soft Delete 되지 않은(deleted_at IS NULL) 데이터만 대상으로 한다. */
    Optional<AiMessage> findByAiMessageIdAndDeletedAtIsNull(UUID aiMessageId);

    /** A1 멱등 처리 / A7 취소 대상 조회용. */
    Optional<AiMessage> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    /**
     * A2. 조건부 검색. 각 조건은 파라미터가 null 이면 통과시키는 방식으로 동적 필터를 흉내낸다.
     * ({@code :param IS NULL OR 컬럼 = :param})
     */
    @Query("""
            SELECT am FROM AiMessage am
            WHERE am.deletedAt IS NULL
              AND (:status IS NULL OR am.status = :status)
              AND (:orderId IS NULL OR am.orderId = :orderId)
              AND (:model IS NULL OR am.model = :model)
            """)
    Page<AiMessage> search(
            @Param("status") AiCallStatus status,
            @Param("orderId") UUID orderId,
            @Param("model") String model,
            Pageable pageable);
}
