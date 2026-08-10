package com.sparta.slack.slack.domain.dto.response;

import com.sparta.slack.slack.domain.entity.SlackMessageType;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * M4. 슬랙 메시지 단건 조회 응답.
 */
@Getter
@Builder
public class SlackMessageFindResponseDto {
    private UUID slackMessageId;
    private String receiverSlackId;
    private String message;
    private SlackMessageType messageType;
    private UUID aiMessageId;
    private UUID referenceId;
    private SlackSendStatus status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String failReason;
    private LocalDateTime sentAt;
}
