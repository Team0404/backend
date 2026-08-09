package com.sparta.slack.ai.domain.dto.response;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A2. AI 호출 실패 메시지 목록의 요소.
 */
@Getter
@Builder
public class AiMessageSummaryResponseDto {
    private UUID aiMessageId;
    private UUID orderId;
    private UUID deliveryId;
    private AiCallStatus status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String failReason;
    private String model;
}
