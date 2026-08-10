package com.sparta.slack.slack.domain.dto.response;

import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * M2. 슬랙 메시지 재발송 응답.
 */
@Getter
@Builder
public class SlackMessageResendResponseDto {
    private UUID slackMessageId;
    private SlackSendStatus status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime sentAt;
}
