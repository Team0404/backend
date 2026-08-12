package com.sparta.slack.slack.repository;

import com.sparta.slack.slack.domain.entity.SlackMessage;
import com.sparta.slack.slack.domain.entity.SlackMessageType;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {

    /** 모든 조회는 Soft Delete 되지 않은(deleted_at IS NULL) 데이터만 대상으로 한다. */
    Optional<SlackMessage> findBySlackMessageIdAndDeletedAtIsNull(UUID slackMessageId);

    /**
     * M3. 조건부 검색. 각 조건은 파라미터가 null 이면 통과시키는 방식으로 동적 필터를 흉내낸다.
     */
    @Query("""
            SELECT sm FROM SlackMessage sm
            WHERE sm.deletedAt IS NULL
              AND (:receiverSlackId IS NULL OR sm.receiverSlackId = :receiverSlackId)
              AND (:messageType IS NULL OR sm.messageType = :messageType)
              AND (:status IS NULL OR sm.status = :status)
            """)
    Page<SlackMessage> search(
            @Param("receiverSlackId") String receiverSlackId,
            @Param("messageType") SlackMessageType messageType,
            @Param("status") SlackSendStatus status,
            Pageable pageable);
}
