package com.sparta.slack.ai.domain.dto.response;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A5. AI 메시지 이력 수정 응답.
 */
@Getter
@Builder
public class AiMessageUpdateResponseDto {
    private UUID aiMessageId;
    private AiCallStatus status;
    private LocalDateTime finalDispatchDeadline;
}
