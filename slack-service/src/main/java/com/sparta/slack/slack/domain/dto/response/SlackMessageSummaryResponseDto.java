package com.sparta.slack.slack.domain.dto.response;

import com.sparta.slack.slack.domain.entity.SlackMessageType;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * M3. 슬랙 메시지 목록/검색의 요소.
 */
@Getter
@Builder
public class SlackMessageSummaryResponseDto {
    private UUID slackMessageId;
    private String receiverSlackId;
    private SlackMessageType messageType;
    private SlackSendStatus status;
    private Integer retryCount;
    private LocalDateTime sentAt;
}
