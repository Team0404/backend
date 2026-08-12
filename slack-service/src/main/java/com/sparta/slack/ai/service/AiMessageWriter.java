package com.sparta.slack.ai.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.slack.ai.domain.dto.request.AiDispatchDeadlineRequestDto;
import com.sparta.slack.ai.domain.entity.AiMessage;
import com.sparta.slack.ai.repository.AiMessageRepository;
import com.sparta.slack.common.exception.MessageErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AiMessage 이력의 영속화만 담당한다.
 */
@Component
@RequiredArgsConstructor
public class AiMessageWriter {

    private final AiMessageRepository aiMessageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage savePending(AiDispatchDeadlineRequestDto request, String requestPrompt) {
        return aiMessageRepository.save(AiMessage.builder()
                .orderId(request.getOrderId())
                .deliveryId(request.getDeliveryId())
                .managerSlackId(request.getManagerSlackId())
                .requestPrompt(requestPrompt)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage markSuccess(
            UUID aiMessageId,
            String responseContent,
            LocalDateTime finalDispatchDeadline,
            String model
    ) {
        AiMessage aiMessage = getOrThrow(aiMessageId);
        aiMessage.markSuccess(responseContent, finalDispatchDeadline, model);
        return aiMessage;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage markFailed(UUID aiMessageId, String failReason) {
        AiMessage aiMessage = getOrThrow(aiMessageId);
        aiMessage.markFailed(failReason);
        return aiMessage;
    }

    /** A4 재생성 진입. 상태 검증(SUCCESS/CANCELLED/한도초과)과 retry_count 증가를 한 트랜잭션에서 처리한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage increaseRetry(UUID aiMessageId) {
        AiMessage aiMessage = getOrThrow(aiMessageId);
        aiMessage.increaseRetry();
        return aiMessage;
    }

    /** 주문 취소 보상. PENDING/FAILED 건만 CANCELLED 로 전이되며, 이미 SUCCESS 인 건은 그대로 둔다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiMessage cancel(UUID aiMessageId) {
        AiMessage aiMessage = getOrThrow(aiMessageId);
        aiMessage.cancel();
        return aiMessage;
    }

    private AiMessage getOrThrow(UUID aiMessageId) {
        return aiMessageRepository.findByAiMessageIdAndDeletedAtIsNull(aiMessageId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.AI_MESSAGE_NOT_FOUND));
    }
}
