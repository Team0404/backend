package com.sparta.slack.ai.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A7. AI 발송시한 취소 요청 (내부).
 * 주문/배송 취소 시 보상 처리를 위해 호출된다.
 */
@Getter
@Builder
public class AiCancelRequestDto {

    @NotNull
    private UUID orderId;

    /** 취소 사유. 이미 알림이 나간 경우 취소 안내 메시지에 포함된다. */
    private String cancelReason;
}
