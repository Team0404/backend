package com.sparta.slack.slack.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.slack.common.exception.MessageErrorCode;
import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.entity.SlackMessage;
import com.sparta.slack.slack.repository.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 슬랙 이력의 영속화만 담당한다.
 *
 * <p>발송 전 PENDING 저장과 발송 후 결과 반영을 각각 <b>독립 트랜잭션</b>으로 끊기 위해
 * 별도 빈으로 분리했다. 같은 클래스 안에서 호출하면 프록시를 타지 않아
 * {@code REQUIRES_NEW} 가 적용되지 않는다.
 *
 * <p>이렇게 끊는 이유:
 * <ul>
 *   <li>Webhook 호출(수백 ms~수 초) 동안 DB 커넥션을 붙잡지 않기 위해</li>
 *   <li>호출 도중 장애가 나도 PENDING 이력이 남아 M2 재발송 대상이 되도록</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SlackMessageWriter {

    private final SlackMessageRepository slackMessageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage savePending(SlackSendCommand command) {
        return slackMessageRepository.save(SlackMessage.builder()
                .receiverSlackId(command.getReceiverSlackId())
                .message(command.getMessage())
                .messageType(command.getMessageType())
                .referenceId(command.getReferenceId())
                .aiMessage(command.getAiMessage())
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage markSuccess(UUID slackMessageId) {
        SlackMessage slackMessage = getOrThrow(slackMessageId);
        slackMessage.markSuccess();
        return slackMessage;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage markFailed(UUID slackMessageId, String failReason) {
        SlackMessage slackMessage = getOrThrow(slackMessageId);
        slackMessage.markFailed(failReason);
        return slackMessage;
    }

    /** M2 재발송 진입. 상태 검증과 retry_count 증가를 한 트랜잭션에서 처리한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlackMessage increaseRetry(UUID slackMessageId) {
        SlackMessage slackMessage = getOrThrow(slackMessageId);
        slackMessage.increaseRetry();
        return slackMessage;
    }

    private SlackMessage getOrThrow(UUID slackMessageId) {
        return slackMessageRepository.findBySlackMessageIdAndDeletedAtIsNull(slackMessageId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.SLACK_MESSAGE_NOT_FOUND));
    }
}
