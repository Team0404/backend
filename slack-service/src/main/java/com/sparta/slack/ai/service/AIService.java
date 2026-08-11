package com.sparta.slack.ai.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.util.PageableUtil;
import com.sparta.slack.ai.client.GeminiClient;
import com.sparta.slack.ai.client.dto.DispatchDeadlineAiResult;
import com.sparta.slack.ai.domain.dto.request.AiCancelRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiDispatchDeadlineRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiMessageRetryRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiMessageUpdateRequestDto;
import com.sparta.slack.ai.domain.dto.response.AiCancelResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiDispatchDeadlineResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageFindResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageRetryResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageSummaryResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageUpdateResponseDto;
import com.sparta.slack.ai.domain.entity.AiCallStatus;
import com.sparta.slack.ai.domain.entity.AiMessage;
import com.sparta.slack.ai.repository.AiMessageRepository;
import com.sparta.slack.common.exception.MessageErrorCode;
import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.domain.entity.SlackMessageType;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import com.sparta.slack.slack.service.SlackSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * AI 발송시한(A1~A7) 비즈니스 로직.
 *
 * <p>클래스 레벨에 {@code @Transactional} 을 걸지 않는다. A1/A4는 Gemini 호출과 슬랙 발송이라는
 * 느린 외부 호출을 포함하는데, 이를 트랜잭션으로 감싸면 그 시간 내내 DB 커넥션을 붙잡게 된다.
 * 영속화는 {@link AiMessageWriter} 가 {@code REQUIRES_NEW} 로 독립 커밋한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    // service
    private final AiMessageWriter aiMessageWriter;
    private final SlackSender slackSender;
    private final GeminiClient geminiClient;

    // repository
    private final AiMessageRepository aiMessageRepository;

    /** 내부 호출(X-Internal-Call)로 A1/A4/A7을 수행할 수 있는 서비스 목록. */
    private static final Set<String> ALLOWED_INTERNAL_SERVICES = Set.of("order-service", "delivery-service");

    private boolean isAllowedInternalCaller(String internalCaller) {
        return internalCaller != null && ALLOWED_INTERNAL_SERVICES.contains(internalCaller);
    }

    /**
     * A1. AI 발송시한 생성·알림 (내부: 배송/주문서비스 or MASTER).
     *
     * <p>Gemini 호출이 실패해도 예외를 던지지 않는다. {@code AiMessage} 를 FAILED 로 남기고
     * 응답에 그 상태를 그대로 반영해야, 이후 A2(실패 목록 조회)/A4(재시도)가 이 건을 찾을 수 있다.
     */
    public ApiResponse<AiDispatchDeadlineResponseDto> dispatchDeadline(
            AiDispatchDeadlineRequestDto request,
            UUID userId,
            UserRole role,
            String internalCaller
    ) {
        authorizeInternal(role, internalCaller);

        String requestPrompt = geminiClient.buildRequestPrompt(request);
        AiMessage aiMessage = aiMessageWriter.savePending(request, requestPrompt);

        DispatchDeadlineAiResult aiResult;
        try {
            aiResult = geminiClient.generateDispatchDeadline(requestPrompt);
        } catch (Exception e) {
            log.error("AI 발송시한 산출 실패. aiMessageId={}, orderId={}",
                    aiMessage.getAiMessageId(), request.getOrderId(), e);
            AiMessage failed = aiMessageWriter.markFailed(aiMessage.getAiMessageId(), toFailReason(e));
            return ApiResponse.success(toDispatchResponse(failed, null, null, null));
        }

        AiMessage succeeded = aiMessageWriter.markSuccess(
                aiMessage.getAiMessageId(),
                aiResult.slackMessage(),
                aiResult.finalDispatchDeadline(),
                geminiClient.getModelName()
        );

        // 발송 허브 담당자에게 알린다. 수신자는 요청에 이미 포함되어 있어 별도 조회가 필요 없다.
        SlackMessageCreateResponseDto sendResult = slackSender.send(SlackSendCommand.builder()
                .receiverSlackId(request.getManagerSlackId())
                .message(aiResult.slackMessage())
                .messageType(SlackMessageType.DISPATCH_DEADLINE)
                .referenceId(request.getDeliveryId())
                .aiMessage(succeeded)
                .build());

        return ApiResponse.success(toDispatchResponse(
                succeeded,
                aiResult.finalDispatchDeadline(),
                sendResult.getSlackMessageId(),
                sendResult.getStatus()
        ));
    }

    /**
     * A2. AI 호출 실패 메시지 목록 (MASTER).
     */
    @Transactional(readOnly = true)
    public PageResponse<AiMessageSummaryResponseDto> searchAiMessages(
            String status,
            UUID orderId,
            String model,
            Pageable pageable,
            UUID userId,
            UserRole role
    ) {
        authorize(role);

        Pageable normalized = PageableUtil.normalize(pageable);
        // status 는 선택 파라미터다. 값이 없으면 null 을 넘겨 쿼리에서 조건 자체를 통과시킨다.
        // (fromString 에 null 을 넣으면 Enum.valueOf 가 NPE 를 던져 400 대신 500 이 나간다)
        AiCallStatus callStatus = blankToNull(status) == null ? null : AiCallStatus.fromString(status);
        Page<AiMessage> page = aiMessageRepository.search(callStatus, orderId, blankToNull(model), normalized);

        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    /**
     * A3. AI 메시지 단건 조회 (MASTER).
     */
    @Transactional(readOnly = true)
    public AiMessageFindResponseDto findAiMessage(UUID aiMessageId, UUID userId, UserRole role) {
        authorize(role);
        AiMessage aiMessage = getAiMessage(aiMessageId);

        return AiMessageFindResponseDto.builder()
                .aiMessageId(aiMessage.getAiMessageId())
                .orderId(aiMessage.getOrderId())
                .deliveryId(aiMessage.getDeliveryId())
                .requestPrompt(aiMessage.getRequestPrompt())
                .responseContent(aiMessage.getResponseContent())
                .finalDispatchDeadline(aiMessage.getFinalDispatchDeadline())
                .model(aiMessage.getModel())
                .failReason(aiMessage.getFailReason())
                .retryCount(aiMessage.getRetryCount())
                .maxRetryCount(aiMessage.getMaxRetryCount())
                .status(aiMessage.getStatus())
                .build();
    }

    /**
     * A4. AI 메시지 재생성(실패 시 재시도) (MASTER 또는 내부 시스템).
     *
     * <p>A1과 마찬가지로 {@code @Transactional} 을 걸지 않는다. Gemini 재호출과 슬랙 재발송이
     * 트랜잭션 안에 들어가면 그 시간 내내 DB 커넥션을 점유한다.
     *
     * <p>재시도 한도 초과·이미 성공·취소된 건은 {@link AiMessage#increaseRetry()} 가 409로 거부한다.
     */
    public AiMessageRetryResponseDto retryAiMessage(
            UUID aiMessageId,
            AiMessageRetryRequestDto request,
            UUID userId,
            UserRole role,
            String internalCaller
    ) {
        authorizeInternal(role, internalCaller);

        // 상태 검증 + retry_count 증가를 먼저 독립 커밋한다.
        // 재호출 도중 장애가 나도 시도 횟수가 남아 무한 재시도를 막는다.
        AiMessage aiMessage = aiMessageWriter.increaseRetry(aiMessageId);
        String requestPrompt = aiMessage.getRequestPrompt();

        DispatchDeadlineAiResult aiResult;
        try {
            aiResult = geminiClient.generateDispatchDeadline(requestPrompt);
        } catch (Exception e) {
            log.error("AI 발송시한 재생성 실패. aiMessageId={}", aiMessageId, e);
            AiMessage failed = aiMessageWriter.markFailed(aiMessageId, toFailReason(e));
            return toRetryResponse(failed, null, null);
        }

        AiMessage succeeded = aiMessageWriter.markSuccess(
                aiMessageId,
                aiResult.slackMessage(),
                aiResult.finalDispatchDeadline(),
                geminiClient.getModelName()
        );

        UUID slackMessageId = shouldResendSlack(request)
                ? resendSlack(succeeded, aiResult)
                : null;

        return toRetryResponse(succeeded, aiResult.finalDispatchDeadline(), slackMessageId);
    }

    /**
     * A5. AI 메시지 이력 수정 (MASTER). AI를 재호출하지 않고 저장된 로그만 정정한다.
     */
    @Transactional
    public AiMessageUpdateResponseDto updateAiMessage(
            UUID aiMessageId,
            AiMessageUpdateRequestDto request,
            UUID userId,
            UserRole role
    ) {
        authorize(role);
        AiMessage aiMessage = getAiMessage(aiMessageId);
        aiMessage.update(
                request.getStatus(),
                request.getFinalDispatchDeadline(),
                request.getResponseContent(),
                request.getFailReason()
        );

        // 부분 수정(PATCH)이므로 요청에 없던 필드는 기존 값이 유지된다.
        // 응답은 요청값이 아니라 반영이 끝난 엔티티에서 읽어야 실제 상태와 일치한다.
        return AiMessageUpdateResponseDto.builder()
                .aiMessageId(aiMessageId)
                .status(aiMessage.getStatus())
                .finalDispatchDeadline(aiMessage.getFinalDispatchDeadline())
                .build();
    }

    /**
     * A6. AI 메시지 이력 삭제 (MASTER). Soft Delete.
     */
    @Transactional
    public ApiResponse<Void> deleteAiMessage(UUID aiMessageId, UUID userId, UserRole role) {
        authorize(role);
        AiMessage aiMessage = getAiMessage(aiMessageId);
        aiMessage.softDelete(userId);
        return ApiResponse.success();
    }

    /**
     * A7. AI 발송시한 취소 (내부: 배송/주문서비스 or MASTER).
     *
     * <p>주문 취소에 대한 보상 처리다. 슬랙은 Incoming Webhook 이라 이미 보낸 메시지를 지울 수 없으므로,
     * "롤백"이 아니라 상태에 따라 두 갈래로 처리한다.
     * <ul>
     *   <li>PENDING/FAILED — 아직 알림이 나가지 않았다. CANCELLED 로 막아 후속 발송·재생성을 차단한다.</li>
     *   <li>SUCCESS — 이미 알림이 나갔다. 되돌릴 수 없으므로 취소 안내를 새로 발송한다.</li>
     * </ul>
     *
     * <p>대상 AI 로그가 없으면(= AI 알림을 쓰지 않은 주문) 조용히 넘어간다. 취소는 멱등해야 하고,
     * 보상 대상이 없다는 이유로 주문 취소 전체가 실패하면 안 되기 때문이다.
     */
    public ApiResponse<AiCancelResponseDto> cancelDispatchDeadline(
            AiCancelRequestDto request,
            UUID userId,
            UserRole role,
            String internalCaller
    ) {
        authorizeInternal(role, internalCaller);

        Optional<AiMessage> found = aiMessageRepository.findByOrderIdAndDeletedAtIsNull(request.getOrderId());
        if (found.isEmpty()) {
            log.info("취소할 AI 발송시한 이력이 없어 건너뛴다. orderId={}", request.getOrderId());
            return ApiResponse.success(null);
        }

        AiMessage aiMessage = found.get();

        // 이미 알림이 나간 건은 상태를 되돌리지 않고 취소 안내를 새로 보낸다.
        if (aiMessage.getStatus() == AiCallStatus.SUCCESS) {
            SlackMessageCreateResponseDto sendResult = slackSender.send(SlackSendCommand.builder()
                    .receiverSlackId(aiMessage.getManagerSlackId())
                    .message(buildCancelMessage(request))
                    .messageType(SlackMessageType.GENERAL)
                    .referenceId(aiMessage.getDeliveryId())
                    .aiMessage(aiMessage)
                    .build());

            return ApiResponse.success(AiCancelResponseDto.builder()
                    .aiMessageId(aiMessage.getAiMessageId())
                    .status(aiMessage.getStatus())
                    .slackMessageId(sendResult.getSlackMessageId())
                    .build());
        }

        AiMessage cancelled = aiMessageWriter.cancel(aiMessage.getAiMessageId());
        return ApiResponse.success(AiCancelResponseDto.builder()
                .aiMessageId(cancelled.getAiMessageId())
                .status(cancelled.getStatus())
                .build());
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────

    /** 재발송 여부. 요청 본문이 없거나 값이 비어 있으면 기본 true. */
    private boolean shouldResendSlack(AiMessageRetryRequestDto request) {
        return request == null || request.getResendSlack() == null || request.getResendSlack();
    }

    /**
     * 재산출된 발송 시한을 발송 허브 담당자에게 다시 알린다.
     *
     * <p>수신자와 참조 ID를 AI 로그에 보관해 두었으므로, A1이 AI 호출 단계에서 실패해
     * 슬랙 이력이 하나도 없는 건도 재발송이 가능하다.
     */
    private UUID resendSlack(AiMessage aiMessage, DispatchDeadlineAiResult aiResult) {
        SlackMessageCreateResponseDto sendResult = slackSender.send(SlackSendCommand.builder()
                .receiverSlackId(aiMessage.getManagerSlackId())
                .message(aiResult.slackMessage())
                .messageType(SlackMessageType.DISPATCH_DEADLINE)
                .referenceId(aiMessage.getDeliveryId())
                .aiMessage(aiMessage)
                .build());

        return sendResult.getSlackMessageId();
    }

    private String buildCancelMessage(AiCancelRequestDto request) {
        String reason = blankToNull(request.getCancelReason()) == null
                ? "사유 미기재"
                : request.getCancelReason();
        return "[발송 시한 알림 취소] 앞서 안내드린 발송 시한은 주문 취소로 무효화되었습니다."
                + " (주문번호: " + request.getOrderId() + " / 사유: " + reason + ")";
    }

    private AiDispatchDeadlineResponseDto toDispatchResponse(
            AiMessage aiMessage,
            LocalDateTime finalDispatchDeadline,
            UUID slackMessageId,
            SlackSendStatus slackStatus
    ) {
        return AiDispatchDeadlineResponseDto.builder()
                .aiMessageId(aiMessage.getAiMessageId())
                .status(aiMessage.getStatus())
                .finalDispatchDeadline(finalDispatchDeadline)
                .slackMessageId(slackMessageId)
                .slackStatus(slackStatus)
                .build();
    }

    private AiMessageRetryResponseDto toRetryResponse(
            AiMessage aiMessage,
            LocalDateTime finalDispatchDeadline,
            UUID slackMessageId
    ) {
        return AiMessageRetryResponseDto.builder()
                .aiMessageId(aiMessage.getAiMessageId())
                .status(aiMessage.getStatus())
                .retryCount(aiMessage.getRetryCount())
                .maxRetryCount(aiMessage.getMaxRetryCount())
                .finalDispatchDeadline(finalDispatchDeadline)
                .slackMessageId(slackMessageId)
                .build();
    }

    private AiMessageSummaryResponseDto toSummaryResponse(AiMessage am) {
        return AiMessageSummaryResponseDto.builder()
                .aiMessageId(am.getAiMessageId())
                .orderId(am.getOrderId())
                .deliveryId(am.getDeliveryId())
                .status(am.getStatus())
                .retryCount(am.getRetryCount())
                .maxRetryCount(am.getMaxRetryCount())
                .failReason(am.getFailReason())
                .model(am.getModel())
                .build();
    }

    /** fail_reason 컬럼이 무한정 길어지지 않도록 잘라서 저장한다. */
    private String toFailReason(Exception e) {
        String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private AiMessage getAiMessage(UUID aiMessageId) {
        return aiMessageRepository.findByAiMessageIdAndDeletedAtIsNull(aiMessageId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.AI_MESSAGE_NOT_FOUND));
    }

    private void authorizeInternal(UserRole role, String internalCaller) {
        if (!UserRole.MASTER.equals(role) && !isAllowedInternalCaller(internalCaller)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void authorize(UserRole role) {
        if (!UserRole.MASTER.equals(role)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
