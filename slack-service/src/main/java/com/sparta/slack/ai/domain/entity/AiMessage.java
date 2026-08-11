package com.sparta.slack.ai.domain.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.common.exception.BusinessException;
import com.sparta.slack.common.exception.MessageErrorCode;
import jakarta.persistence.*;
import lombok.*;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AiMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID aiMessageId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    /**
     * 알림을 받을 발송 허브 담당자의 슬랙 ID.
     *
     * <p>배송의 {@code recipient_slack_id}(수령 고객)와는 다른 대상이다. A4 재생성 성공 시
     * 슬랙을 재발송하려면 수신자를 알아야 하는데, A1이 AI 호출 단계에서 실패하면 슬랙 이력이
     * 아예 생기지 않아 이력에서 역추적할 수 없다. 그래서 AI 로그 자체에 보관한다.
     */
    @Column(name = "manager_slack_id", nullable = false, length = 100)
    private String managerSlackId;

    @Column(name = "request_prompt", nullable = false, columnDefinition = "TEXT")
    private String requestPrompt;

    @Column(name = "response_content", columnDefinition = "TEXT")
    private String responseContent;

    @Column(name = "final_dispatch_deadline")
    private LocalDateTime finalDispatchDeadline;

    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "fail_reason", columnDefinition = "TEXT")
    private String failReason;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Builder.Default
    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 3;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiCallStatus status = AiCallStatus.PENDING;

    /**
     * AI 호출 성공 반영. 이전 실패 사유는 지운다.
     */
    public void markSuccess(String responseContent, LocalDateTime finalDispatchDeadline, String model) {
        this.responseContent = responseContent;
        this.finalDispatchDeadline = finalDispatchDeadline;
        this.model = model;
        this.failReason = null;
        this.status = AiCallStatus.SUCCESS;
    }

    /**
     * AI 호출 실패 반영. 재시도 한도가 남아 있으면 A4(재생성) 대상이 된다.
     */
    public void markFailed(String failReason) {
        this.failReason = failReason;
        this.status = AiCallStatus.FAILED;
    }

    /**
     * 주문 취소에 따른 무효화. 아직 발송 전(PENDING)인 건의 슬랙 발송을 억제한다.
     * 이미 SUCCESS 로 알림이 나간 건은 되돌릴 수 없으므로 상태를 바꾸지 않고,
     * 호출 측에서 취소 알림을 별도로 발송한다.
     */
    public void cancel() {
        if (this.status == AiCallStatus.PENDING || this.status == AiCallStatus.FAILED) {
            this.status = AiCallStatus.CANCELLED;
        }
    }

    public boolean isCancelled() {
        return this.status == AiCallStatus.CANCELLED;
    }

    /**
     * A4 재생성 진입 시 호출. 이미 성공했거나 취소됐거나 한도를 넘겼으면 409로 거부한다.
     */
    public void increaseRetry() {
        if (this.status == AiCallStatus.SUCCESS) {
            throw new BusinessException(MessageErrorCode.AI_MESSAGE_ALREADY_SUCCEEDED);
        }
        if (this.status == AiCallStatus.CANCELLED) {
            throw new BusinessException(MessageErrorCode.AI_MESSAGE_CANCELLED);
        }
        if (!canRetry()) {
            throw new BusinessException(MessageErrorCode.AI_RETRY_LIMIT_EXCEEDED);
        }
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }

    /**
     * A5 이력 정정. AI를 재호출하지 않고 저장된 로그만 수정한다.
     * (실제 재호출은 A4 재생성)
     */
    public void update(
            String status,
            LocalDateTime finalDispatchDeadline,
            String responseContent,
            String failReason
    ) {
        if (status != null && !status.isBlank()) {
            this.status = AiCallStatus.fromString(status);
        }
        if (finalDispatchDeadline != null) {
            this.finalDispatchDeadline = finalDispatchDeadline;
        }
        if (responseContent != null && !responseContent.isBlank()) {
            this.responseContent = responseContent;
        }
        if (failReason != null && !failReason.isBlank()) {
            this.failReason = failReason;
        }
    }
}
