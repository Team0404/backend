package com.sparta.delivery.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.client.AiClient;
import com.sparta.delivery.client.HubClient;
import com.sparta.delivery.client.UserClient;
import com.sparta.delivery.client.dto.AiDispatchDeadlineRequest;
import com.sparta.delivery.client.dto.HubResponse;
import com.sparta.delivery.config.AsyncConfig;
import com.sparta.delivery.domain.event.DeliveryCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 배송 생성 후 AI 발송시한 알림(A1)을 트리거하는 리스너.
 *
 * <p><b>{@code AFTER_COMMIT}</b> — 배송 저장이 커밋된 뒤에만 실행된다. 저장이 롤백되면
 * 알림도 나가지 않는다.
 *
 * <p><b>{@code @Async}</b> — 별도 스레드로 넘겨 D1 응답이 AI 호출을 기다리지 않게 한다.
 * AI 알림은 배송 성립의 전제 조건이 아니므로 Saga 참여자로 두지 않는다.
 *
 * <p><b>예외를 밖으로 던지지 않는다.</b> {@code @Async} 에서 발생한 예외는 호출자에게
 * 전파되지 않고 그대로 소멸하므로, 여기서 잡아 로그로 남겨야 원인을 추적할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchDeadlineEventListener {

    private static final String DEFAULT_WORKING_HOURS = "09-18";

    private final AiClient aiClient;
    private final HubClient hubClient;
    private final UserClient userClient;

    @Async(AsyncConfig.DISPATCH_DEADLINE_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryCreated(DeliveryCreatedEvent event) {
        try {
            String managerSlackId = resolveManagerSlackId(event.hubDeliveryManagerId());
            if (managerSlackId == null) {
                // A1은 managerSlackId가 필수다. 받을 사람을 특정할 수 없으면 호출 자체를 건너뛴다.
                log.warn("발송 허브 담당자의 슬랙 ID를 확인할 수 없어 AI 발송시한 알림을 건너뛴다. deliveryId={}, managerId={}",
                        event.deliveryId(), event.hubDeliveryManagerId());
                return;
            }

            String originHubName = resolveHubName(event.originHubId());
            if (originHubName == null) {
                log.warn("출발 허브 정보를 조회할 수 없어 AI 발송시한 알림을 건너뛴다. deliveryId={}, originHubId={}",
                        event.deliveryId(), event.originHubId());
                return;
            }

            aiClient.dispatchDeadline(new AiDispatchDeadlineRequest(
                    event.orderId(),
                    event.deliveryId(),
                    event.productInfo(),
                    event.requestNote(),
                    originHubName,
                    resolveWaypointHubNames(event.waypointHubIds()),
                    event.destAddress(),
                    managerSlackId,
                    DEFAULT_WORKING_HOURS
            ));

            log.info("AI 발송시한 알림 요청 완료. deliveryId={}, orderId={}", event.deliveryId(), event.orderId());
        } catch (Exception e) {
            // 알림 실패가 배송 생성에 영향을 주면 안 된다. 로그만 남기고 삼킨다.
            // 실패 건은 slack-service의 A2(실패 목록)/A4(재생성)로 후속 처리한다.
            log.error("AI 발송시한 알림 실패. deliveryId={}, orderId={}", event.deliveryId(), event.orderId(), e);
        }
    }

    private String resolveManagerSlackId(UUID hubDeliveryManagerId) {
        if (hubDeliveryManagerId == null) {
            return null;
        }
        ApiResponse<UserInfoResponse> response = userClient.getUser(hubDeliveryManagerId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return null;
        }
        String slackId = response.getData().getSlackId();
        return (slackId == null || slackId.isBlank()) ? null : slackId;
    }

    private String resolveHubName(UUID hubId) {
        if (hubId == null) {
            return null;
        }
        ResponseEntity<ApiResponse<HubResponse>> response = hubClient.findHub(hubId);
        if (response == null || response.getBody() == null
                || !response.getBody().isSuccess() || response.getBody().getData() == null) {
            return null;
        }
        return response.getBody().getData().getName();
    }

    /**
     * 경유 허브 이름 목록. 경로 생성이 아직 구현되지 않아 대부분 비어 있으며,
     * slack-service 쪽에서 비어 있으면 "경유지 없음"으로 처리한다.
     */
    private List<String> resolveWaypointHubNames(List<UUID> waypointHubIds) {
        if (waypointHubIds == null || waypointHubIds.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (UUID hubId : waypointHubIds) {
            String name = resolveHubName(hubId);
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }
}
