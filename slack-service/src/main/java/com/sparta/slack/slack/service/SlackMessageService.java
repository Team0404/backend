package com.sparta.slack.slack.service;

import com.sparta.common.response.ApiResponse;
import com.sparta.slack.slack.client.SlackWebhookClient;
import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.request.SlackMessageCreateRequestDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.domain.entity.SlackMessage;
import com.sparta.slack.slack.domain.entity.SlackMessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 슬랙 메시지(M1~M6) 비즈니스 로직 + {@link SlackSender} 구현.
 *
 * <p>클래스 레벨에 {@code @Transactional} 을 걸지 않는다. Webhook 호출이
 * 트랜잭션 밖에서 일어나야 하며, 영속화는 {@link SlackMessageWriter} 가
 * 독립 트랜잭션으로 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackMessageService implements SlackSender {

    private final SlackMessageWriter slackMessageWriter;
    private final SlackWebhookClient slackWebhookClient;

    /**
     * M1. 슬랙 메시지 생성·발송.
     * 로그인한 모든 사용자 및 내부 시스템이 호출할 수 있어 별도 역할 검증은 하지 않는다.
     */
    public ApiResponse<SlackMessageCreateResponseDto> createSlackMessage(SlackMessageCreateRequestDto request) {
        SlackSendCommand command = SlackSendCommand.builder()
                .receiverSlackId(request.getReceiverSlackId())
                .message(request.getMessage())
                .messageType(resolveMessageType(request.getMessageType()))
                .referenceId(request.getReferenceId())
                .build();

        return ApiResponse.success(send(command));
    }

    /**
     * 발송 진입점. AI(A1/A4)도 이 메서드를 통해 발송한다.
     *
     * <p>발송에 실패해도 예외를 던지지 않는다. 이력을 FAILED 로 남기고 그대로 반환해야
     * 호출자(A1)의 AI 로그가 함께 커밋되고, 이후 M2 재발송이 가능해진다.
     */
    @Override
    public SlackMessageCreateResponseDto send(SlackSendCommand command) {
        SlackMessage saved = slackMessageWriter.savePending(command);

        SlackMessage result;
        try {
            slackWebhookClient.send(command.getReceiverSlackId(), command.getMessage());
            result = slackMessageWriter.markSuccess(saved.getSlackMessageId());
        } catch (Exception e) {
            log.error("슬랙 발송 실패. slackMessageId={}, receiverSlackId={}",
                    saved.getSlackMessageId(), command.getReceiverSlackId(), e);
            result = slackMessageWriter.markFailed(saved.getSlackMessageId(), toFailReason(e));
        }

        return toCreateResponse(result);
    }

    private SlackMessageType resolveMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return SlackMessageType.GENERAL;
        }
        return SlackMessageType.fromString(messageType);
    }

    /** fail_reason 컬럼이 무한정 길어지지 않도록 잘라서 저장한다. */
    private String toFailReason(Exception e) {
        String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    private SlackMessageCreateResponseDto toCreateResponse(SlackMessage slackMessage) {
        return SlackMessageCreateResponseDto.builder()
                .slackMessageId(slackMessage.getSlackMessageId())
                .status(slackMessage.getStatus())
                .retryCount(slackMessage.getRetryCount())
                .sentAt(slackMessage.getSentAt())
                .build();
    }
}
