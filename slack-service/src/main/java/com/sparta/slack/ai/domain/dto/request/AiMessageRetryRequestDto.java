package com.sparta.slack.ai.domain.dto.request;

import lombok.Builder;
import lombok.Getter;

/**
 * A4. AI 메시지 재생성 요청.
 * 저장된 프롬프트로 AI를 재호출한다.
 */
@Getter
@Builder
public class AiMessageRetryRequestDto {

    private Boolean resendSlack;
}
