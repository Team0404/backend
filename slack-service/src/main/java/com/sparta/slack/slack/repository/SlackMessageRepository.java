package com.sparta.slack.slack.repository;

import com.sparta.slack.slack.domain.entity.SlackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, UUID> {

    /** 모든 조회는 Soft Delete 되지 않은(deleted_at IS NULL) 데이터만 대상으로 한다. */
    Optional<SlackMessage> findBySlackMessageIdAndDeletedAtIsNull(UUID slackMessageId);
}
