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
 * AI 요청/응답 로그 (p_ai_messages).
 *
 * 주문 생성 시 발송 시한 산출을 위해 AI(Gemini)를 호출한 이력을 저장한다.
 * 실패 시 FAILED + fail_reason 을 기록하며 retry_count 가 max_retry_count 에
 * 도달하면 최종 FAILED 로 확정된다.
 */
@Entity
@Table(name = "p_ai_messages")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AiMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID id;

    @Column(name = "order_id")
    public UUID orderId;

    @Column(name = "delivery_id", nullable = false)
    public UUID deliveryId;

    @Column(name = "request_prompt", nullable = false, columnDefinition = "TEXT")
    public String requestPrompt;

    @Column(name = "response_content", columnDefinition = "TEXT")
    public String responseContent;

    @Column(name = "final_dispatch_deadline")
    public LocalDateTime finalDispatchDeadline;

    @Column(name = "model", length = 50)
    public String model;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    public String failReason;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    public Integer retryCount = 0;

    @Builder.Default
    @Column(name = "max_retry_count", nullable = false)
    public Integer maxRetryCount = 3;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public AiCallStatus status = AiCallStatus.PENDING;
}
