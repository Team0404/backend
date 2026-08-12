package com.sparta.order.client.dto;

import java.util.UUID;

/**
 * 배송 생성(D1) 요청 본문.
 *
 * <p>{@code productInfo}/{@code requestNote} 는 배송 도메인이 쓰는 값이 아니라,
 * 배송 서비스가 AI 발송시한 알림(A1)을 호출할 때 프롬프트에 넣기 위해 함께 전달한다.
 * 배송이 주문을 역으로 조회하는 방식은 쓸 수 없다 — 이 호출은 주문 트랜잭션이
 * 커밋되기 전에 일어나므로 아직 저장되지 않은 주문을 읽게 된다.
 */
public record DeliveryCreateRequest(
        UUID orderId,
        UUID originHubId,
        UUID destHubId,
        String deliveryAddress,
        String recipientName,
        String recipientSlackId,
        String productInfo,
        String requestNote
) {
}
