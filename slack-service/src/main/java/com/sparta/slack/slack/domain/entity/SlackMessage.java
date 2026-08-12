package com.sparta.slack.slack.domain.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.common.exception.BusinessException;
import com.sparta.slack.ai.domain.entity.AiMessage;
import com.sparta.slack.common.exception.MessageErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 슬랙 메시지 (p_slack_messages).
 *
 * Incoming Webhook 으로 발송한 메시지 이력을 저장한다. AI 가 생성한 메시지는
 * ai_message 로 연결(스키마 내부 물리 FK)되며, reference_id 는 관련 주문/배송 ID
 * (타 서비스 논리 참조)이다. 발송 실패 시 재발송 대상이 된다.
 */
@Entity
@Table(name = "p_slack_messages")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SlackMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID slackMessageId;

    @Column(name = "receiver_slack_id", nullable = false, length = 100)
    private String receiverSlackId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private SlackMessageType messageType = SlackMessageType.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_message_id")
    private AiMessage aiMessage;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlackSendStatus status = SlackSendStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Builder.Default
    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 발송 성공 반영. 이전 실패 사유는 지운다.
     */
    public void markSuccess() {
        this.status = SlackSendStatus.SUCCESS;
        this.sentAt = LocalDateTime.now();
        this.failReason = null;
    }

    /**
     * 발송 실패 반영. 재시도 한도가 남아 있으면 M2(재발송) 대상이 된다.
     */
    public void markFailed(String failReason) {
        this.status = SlackSendStatus.FAILED;
        this.failReason = failReason;
    }

    /**
     * M2 재발송 진입 시 호출. 이미 성공했거나 한도를 넘겼으면 409로 거부한다.
     */
    public void increaseRetry() {
        if (this.status == SlackSendStatus.SUCCESS) {
            throw new BusinessException(MessageErrorCode.SLACK_MESSAGE_ALREADY_SENT);
        }
        if (!canRetry()) {
            throw new BusinessException(MessageErrorCode.SLACK_RETRY_LIMIT_EXCEEDED);
        }
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }

    /**
     * M5 이력 정정. Incoming Webhook 방식이라 슬랙 채널의 실제 메시지는 바뀌지 않고,
     * 우리 DB에 저장된 이력만 수정한다.
     */
    public void update(
            String message,
            String messageType,
            String status,
            UUID referenceId
    ) {
        if (message != null && !message.isBlank()) {
            this.message = message;
        }
        if (messageType != null && !messageType.isBlank()) {
            this.messageType = SlackMessageType.fromString(messageType);
        }
        if (status != null && !status.isBlank()) {
            this.status = SlackSendStatus.fromString(status);
        }
        if (referenceId != null) {
            this.referenceId = referenceId;
        }
    }
}
