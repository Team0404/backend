package com.sparta.slack.ai.domain.dto.response;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A4. AI 메시지 재생성 응답.
 */
@Getter
@Builder
public class AiMessageRetryResponseDto {
    private UUID aiMessageId;
    private AiCallStatus status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime finalDispatchDeadline;
    /** 재발송한 경우에만 채워진다 */
    private UUID slackMessageId;
}
