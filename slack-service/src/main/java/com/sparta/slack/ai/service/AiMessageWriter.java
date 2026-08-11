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
 *
 * <p>Gemini 호출(수백 ms~수십 초) 동안 DB 커넥션을 붙잡지 않기 위해, 호출 전 PENDING 저장과
 * 호출 후 결과 반영을 각각 <b>독립 트랜잭션</b>으로 끊는다. 같은 클래스 안에서 호출하면
 * self-invocation 으로 프록시를 타지 않아 {@code REQUIRES_NEW} 가 적용되지 않으므로,
 * {@link com.sparta.slack.slack.service.SlackMessageWriter} 와 동일하게 별도 빈으로 분리했다.
 *
 * <p>PENDING 저장을 먼저 커밋해 두는 이유: 호출 도중 서버가 죽어도 이력이 남아
 * A2(실패 목록)/A4(재생성) 대상으로 잡히도록 하기 위함이다.
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
