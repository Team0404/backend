package com.sparta.slack.ai.domain.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * A5. AI 메시지 이력 수정 요청.
 * AI를 재호출하지 않고 저장된 로그만 정정한다. (재호출은 A4)
 */
@Getter
@Builder
public class AiMessageUpdateRequestDto {
    private String status;
    private LocalDateTime finalDispatchDeadline;
    private String responseContent;
    private String failReason;
}
