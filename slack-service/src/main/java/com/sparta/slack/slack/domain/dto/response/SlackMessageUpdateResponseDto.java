package com.sparta.slack.slack.domain.dto.response;

import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * M5. 슬랙 메시지 이력 수정 응답.
 */
@Getter
@Builder
public class SlackMessageUpdateResponseDto {
    private UUID slackMessageId;
    private SlackSendStatus status;
}
