package com.sparta.delivery.client.dto;

import java.util.UUID;

/**
 * slack-service A7(AI 발송시한 취소) 요청 본문.
 */
public record AiCancelRequest(
        UUID orderId,
        String cancelReason
) {
}
