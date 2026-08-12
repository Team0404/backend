package com.sparta.slack.slack.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.util.PageableUtil;
import com.sparta.slack.common.exception.MessageErrorCode;
import com.sparta.slack.slack.client.SlackWebhookClient;
import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.request.SlackMessageCreateRequestDto;
import com.sparta.slack.slack.domain.dto.request.SlackMessageUpdateRequestDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageFindResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageSummaryResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageUpdateResponseDto;
import com.sparta.slack.slack.domain.entity.SlackMessage;
import com.sparta.slack.slack.domain.entity.SlackMessageType;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import com.sparta.slack.slack.repository.SlackMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    private final SlackMessageRepository slackMessageRepository;

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

    /**
     * M2. 슬랙 메시지 재발송 (MASTER).
     *
     * <p>같은 이력 행에 결과를 갱신한다. {@link #send} 는 새 이력을 만드는 M1 전용 경로라 여기선
     * 재사용하지 않는다. retry_count 증가와 상태 검증(이미 SUCCESS/한도초과 시 409)은
     * {@link SlackMessage#increaseRetry()} 가 처리하고, Webhook 호출 자체의 일시 오류에 대한
     * 즉시 재시도는 {@link SlackWebhookClient} 가 그대로 적용한다 — 여기서 별도로 재시도를
     * 걸면 한 번의 M2 호출로 시도 횟수가 이중으로 늘어난다.
     */
    public ApiResponse<SlackMessageCreateResponseDto> resendSlackMessage(UUID slackMessageId, UserRole role) {
        authorize(role);

        SlackMessage target = slackMessageWriter.increaseRetry(slackMessageId);

        SlackMessage result;
        try {
            slackWebhookClient.send(target.getReceiverSlackId(), target.getMessage());
            result = slackMessageWriter.markSuccess(slackMessageId);
        } catch (Exception e) {
            log.error("슬랙 재발송 실패. slackMessageId={}, receiverSlackId={}",
                    slackMessageId, target.getReceiverSlackId(), e);
            result = slackMessageWriter.markFailed(slackMessageId, toFailReason(e));
        }

        return ApiResponse.success(toCreateResponse(result));
    }

    /**
     * M3. 슬랙 메시지 목록/검색 (MASTER).
     */
    @Transactional(readOnly = true)
    public PageResponse<SlackMessageSummaryResponseDto> searchSlackMessages(
            String receiverSlackId,
            String messageType,
            String status,
            Pageable pageable,
            UserRole role
    ) {
        authorize(role);

        Pageable normalized = PageableUtil.normalize(pageable);
        SlackMessageType type = blankToNull(messageType) == null ? null : SlackMessageType.fromString(messageType);
        SlackSendStatus sendStatus = blankToNull(status) == null ? null : SlackSendStatus.fromString(status);
        Page<SlackMessage> page = slackMessageRepository.search(
                blankToNull(receiverSlackId), type, sendStatus, normalized
        );

        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    /**
     * M4. 슬랙 메시지 단건 조회 (MASTER).
     */
    @Transactional(readOnly = true)
    public SlackMessageFindResponseDto findSlackMessage(UUID slackMessageId, UserRole role) {
        authorize(role);
        SlackMessage slackMessage = getSlackMessage(slackMessageId);

        return SlackMessageFindResponseDto.builder()
                .slackMessageId(slackMessage.getSlackMessageId())
                .receiverSlackId(slackMessage.getReceiverSlackId())
                .message(slackMessage.getMessage())
                .messageType(slackMessage.getMessageType())
                .aiMessageId(slackMessage.getAiMessage() == null ? null : slackMessage.getAiMessage().getAiMessageId())
                .referenceId(slackMessage.getReferenceId())
                .status(slackMessage.getStatus())
                .retryCount(slackMessage.getRetryCount())
                .maxRetryCount(slackMessage.getMaxRetryCount())
                .failReason(slackMessage.getFailReason())
                .sentAt(slackMessage.getSentAt())
                .build();
    }

    /**
     * M5. 슬랙 메시지 이력 수정 (MASTER).
     *
     * <p>Incoming Webhook 방식이라 슬랙 채널에 이미 발송된 실제 메시지는 수정되지 않는다.
     * 우리 DB에 저장된 이력만 정정한다.
     */
    @Transactional
    public SlackMessageUpdateResponseDto updateSlackMessage(
            UUID slackMessageId,
            SlackMessageUpdateRequestDto request,
            UserRole role
    ) {
        authorize(role);
        SlackMessage slackMessage = getSlackMessage(slackMessageId);
        slackMessage.update(
                request.getMessage(),
                request.getMessageType(),
                request.getStatus(),
                request.getReferenceId()
        );

        // 부분 수정(PATCH)이므로 반영이 끝난 엔티티에서 읽어야 실제 상태와 일치한다.
        return SlackMessageUpdateResponseDto.builder()
                .slackMessageId(slackMessageId)
                .status(slackMessage.getStatus())
                .build();
    }

    /**
     * M6. 슬랙 메시지 이력 삭제 (MASTER). Soft Delete.
     */
    @Transactional
    public ApiResponse<Void> deleteSlackMessage(UUID slackMessageId, UUID userId, UserRole role) {
        authorize(role);
        SlackMessage slackMessage = getSlackMessage(slackMessageId);
        slackMessage.softDelete(userId);
        return ApiResponse.success();
    }

    private SlackMessageSummaryResponseDto toSummaryResponse(SlackMessage sm) {
        return SlackMessageSummaryResponseDto.builder()
                .slackMessageId(sm.getSlackMessageId())
                .receiverSlackId(sm.getReceiverSlackId())
                .messageType(sm.getMessageType())
                .status(sm.getStatus())
                .retryCount(sm.getRetryCount())
                .sentAt(sm.getSentAt())
                .build();
    }

    private SlackMessage getSlackMessage(UUID slackMessageId) {
        return slackMessageRepository.findBySlackMessageIdAndDeletedAtIsNull(slackMessageId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.SLACK_MESSAGE_NOT_FOUND));
    }

    private void authorize(UserRole role) {
        if (role != UserRole.MASTER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String blankToNull(String str) {
        return (str == null || str.isBlank()) ? null : str;
    }
}
