package com.sparta.slack.domain.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SlackMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @Column(name = "receiver_slack_id", nullable = false, length = 100)
    public String receiverSlackId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    public String message;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    public SlackMessageType messageType = SlackMessageType.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_message_id")
    public AiMessage aiMessage;

    @Column(name = "reference_id")
    public UUID referenceId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public SlackSendStatus status = SlackSendStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    public Integer retryCount = 0;

    @Builder.Default
    @Column(name = "max_retry_count", nullable = false)
    public Integer maxRetryCount = 3;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    public String failReason;

    @Column(name = "sent_at")
    public LocalDateTime sentAt;
}
