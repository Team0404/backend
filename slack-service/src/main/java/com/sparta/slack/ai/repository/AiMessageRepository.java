package com.sparta.slack.ai.repository;

import com.sparta.slack.ai.domain.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    /** 모든 조회는 Soft Delete 되지 않은(deleted_at IS NULL) 데이터만 대상으로 한다. */
    Optional<AiMessage> findByAiMessageIdAndDeletedAtIsNull(UUID aiMessageId);

    /** A1 멱등 처리용. 동일 주문으로 이미 발송 시한이 생성됐는지 확인한다. */
    Optional<AiMessage> findByOrderIdAndDeletedAtIsNull(UUID orderId);
}
