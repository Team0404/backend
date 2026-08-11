package com.sparta.delivery.domain.event;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * 배송 생성 완료 이벤트. 커밋 이후 AI 발송시한 알림(A1)을 트리거한다.
 *
 * <p>리스너가 {@code @Async} 로 다른 스레드에서 실행되므로 <b>엔티티를 담지 않는다.</b>
 * 스레드가 바뀌면 영속성 컨텍스트가 끊겨 지연 로딩이 터지기 때문에, 필요한 값은
 * 전부 원시 데이터로 복사해 둔다.
 */
@Builder
public record DeliveryCreatedEvent(
        UUID orderId,
        UUID deliveryId,
        UUID originHubId,
        String destAddress,
        String productInfo,
        String requestNote,
        /** 알림 대상인 허브 배송담당자 userId. 배정 가능한 담당자가 없으면 null. */
        UUID hubDeliveryManagerId,
        /** 경유 허브 ID 목록(순서대로). 경로 생성 전이면 비어 있다. */
        List<UUID> waypointHubIds
) {
}
