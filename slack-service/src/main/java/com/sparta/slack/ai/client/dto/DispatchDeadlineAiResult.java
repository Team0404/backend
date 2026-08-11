package com.sparta.slack.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDateTime;

/** Gemini의 구조화 응답(structured output) 결과. */
public record DispatchDeadlineAiResult(
        @JsonPropertyDescription("납기에 맞추기 위해 발송 허브에서 발송해야 하는 최종 시한. ISO-8601 형식(예: 2025-12-10T09:00:00)의 미래 시각.")
        LocalDateTime finalDispatchDeadline,

        @JsonPropertyDescription("발송 허브 담당자에게 그대로 슬랙으로 전송할 한국어 안내 메시지. "
                + "주문 정보 요약과 최종 발송 시한 안내 문장을 포함한다.")
        String slackMessage
) {
}
