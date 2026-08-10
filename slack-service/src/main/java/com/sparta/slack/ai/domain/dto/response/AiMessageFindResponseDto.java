package com.sparta.slack.ai.domain.dto.response;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A3. AI 메시지 단건 조회 응답.
 */
@Getter
@Builder
public class AiMessageFindResponseDto {
    private UUID aiMessageId;
    private UUID orderId;
    private UUID deliveryId;
    private String requestPrompt;
    private String responseContent;
    private LocalDateTime finalDispatchDeadline;
    private String model;
    private String failReason;
    private Integer retryCount;
    private Integer maxRetryCount;
    private AiCallStatus status;
}
