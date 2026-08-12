package com.sparta.delivery.client.dto;

import java.util.List;
import java.util.UUID;

/**
 * slack-service A1(AI 발송시한 생성·알림) 요청 본문.
 */
public record AiDispatchDeadlineRequest(
        UUID orderId,
        UUID deliveryId,
        String productInfo,
        String requestNote,
        String originHubName,
        List<String> waypointHubNames,
        String destAddress,
        String managerSlackId,
        String workingHours
) {
}
