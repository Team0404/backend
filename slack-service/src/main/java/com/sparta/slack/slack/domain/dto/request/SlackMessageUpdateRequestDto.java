package com.sparta.slack.slack.domain.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * M5. 슬랙 메시지 이력 수정 요청.
 * Incoming Webhook 방식이라 슬랙 채널의 실제 메시지는 수정되지 않고,
 * 우리 DB의 이력 레코드만 정정한다.
 */
@Getter
@Builder
public class SlackMessageUpdateRequestDto {
    private String message;
    private String messageType;
    private String status;
    private UUID referenceId;
}
