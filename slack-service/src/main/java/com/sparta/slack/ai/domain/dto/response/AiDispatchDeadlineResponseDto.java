package com.sparta.slack.ai.domain.dto.response;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A1. AI 발송시한 생성·알림 응답.
 */
@Getter
@Builder
public class AiDispatchDeadlineResponseDto {
    private UUID aiMessageId;
    private AiCallStatus status;
    private LocalDateTime finalDispatchDeadline;
    private UUID slackMessageId;
    private SlackSendStatus slackStatus;
}
