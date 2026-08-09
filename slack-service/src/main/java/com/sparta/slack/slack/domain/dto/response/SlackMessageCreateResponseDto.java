package com.sparta.slack.slack.domain.dto.response;

import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * M1. 슬랙 메시지 생성·발송 응답.
 * A1/A4에서 슬랙 발송 결과를 돌려받을 때도 재사용한다.
 */
@Getter
@Builder
public class SlackMessageCreateResponseDto {
    private UUID slackMessageId;
    private SlackSendStatus status;
    private Integer retryCount;
    private LocalDateTime sentAt;
}
