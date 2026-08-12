package com.sparta.slack.ai.domain.dto.response;

import com.sparta.slack.ai.domain.entity.AiCallStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A7. AI 발송시한 취소 응답.
 */
@Getter
@Builder
public class AiCancelResponseDto {
    private UUID aiMessageId;
    private AiCallStatus status;
    /** 이미 알림이 나가 취소 안내를 재발송한 경우에만 채워진다. */
    private UUID slackMessageId;
}
