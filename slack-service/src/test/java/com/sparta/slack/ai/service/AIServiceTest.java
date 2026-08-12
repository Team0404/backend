package com.sparta.slack.ai.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.slack.ai.client.GeminiClient;
import com.sparta.slack.ai.client.dto.DispatchDeadlineAiResult;
import com.sparta.slack.ai.domain.dto.request.AiCancelRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiDispatchDeadlineRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiMessageRetryRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiMessageUpdateRequestDto;
import com.sparta.slack.ai.domain.dto.response.AiCancelResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiDispatchDeadlineResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageRetryResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageUpdateResponseDto;
import com.sparta.slack.ai.domain.entity.AiCallStatus;
import com.sparta.slack.ai.domain.entity.AiMessage;
import com.sparta.slack.ai.repository.AiMessageRepository;
import com.sparta.slack.common.exception.MessageErrorCode;
import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import com.sparta.slack.slack.service.SlackSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIServiceTest {

    private AiMessageWriter aiMessageWriter;
    private SlackSender slackSender;
    private GeminiClient geminiClient;
    private AiMessageRepository aiMessageRepository;
    private AIService aiService;

    @BeforeEach
    void setUp() {
        aiMessageWriter = mock(AiMessageWriter.class);
        slackSender = mock(SlackSender.class);
        geminiClient = mock(GeminiClient.class);
        aiMessageRepository = mock(AiMessageRepository.class);
        aiService = new AIService(aiMessageWriter, slackSender, geminiClient, aiMessageRepository);
    }

    // ── A1 발송시한 생성 ────────────────────────────────────────────

    @Test
    void dispatchDeadline_deniedForNonMasterNonInternal() {
        AiDispatchDeadlineRequestDto request = createDispatchRequest();

        assertThatThrownBy(() -> aiService.dispatchDeadline(request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    @Test
    void dispatchDeadline_aiSuccess_marksSuccessAndSendsSlack() {
        AiDispatchDeadlineRequestDto request = createDispatchRequest();
        AiMessage pending = pendingAiMessage();
        AiMessage succeeded = succeededAiMessage();
        DispatchDeadlineAiResult aiResult = new DispatchDeadlineAiResult(LocalDateTime.now().plusDays(1), "안내 메시지");

        when(geminiClient.buildRequestPrompt(request)).thenReturn("prompt");
        when(aiMessageWriter.savePending(request, "prompt")).thenReturn(pending);
        when(geminiClient.generateDispatchDeadline("prompt")).thenReturn(aiResult);
        when(geminiClient.getModelName()).thenReturn("gemini-flash-latest");
        when(aiMessageWriter.markSuccess(eq(pending.getAiMessageId()), anyString(), any(), anyString()))
                .thenReturn(succeeded);
        when(slackSender.send(any(SlackSendCommand.class))).thenReturn(SlackMessageCreateResponseDto.builder()
                .slackMessageId(UUID.randomUUID())
                .status(SlackSendStatus.SUCCESS)
                .build());

        ApiResponse<AiDispatchDeadlineResponseDto> response =
                aiService.dispatchDeadline(request, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.getData().getStatus()).isEqualTo(AiCallStatus.SUCCESS);
        verify(slackSender).send(any(SlackSendCommand.class));
    }

    @Test
    void dispatchDeadline_aiCallFails_marksFailedWithoutThrowingAndSkipsSlack() {
        AiDispatchDeadlineRequestDto request = createDispatchRequest();
        AiMessage pending = pendingAiMessage();
        AiMessage failed = failedAiMessage();

        when(geminiClient.buildRequestPrompt(request)).thenReturn("prompt");
        when(aiMessageWriter.savePending(request, "prompt")).thenReturn(pending);
        when(geminiClient.generateDispatchDeadline("prompt"))
                .thenThrow(new BusinessException(MessageErrorCode.AI_CALL_FAILED));
        when(aiMessageWriter.markFailed(eq(pending.getAiMessageId()), anyString())).thenReturn(failed);

        ApiResponse<AiDispatchDeadlineResponseDto> response =
                aiService.dispatchDeadline(request, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(AiCallStatus.FAILED);
        verify(slackSender, never()).send(any());
    }

    @Test
    void dispatchDeadline_internalCallerAllowedRegardlessOfRole() {
        AiDispatchDeadlineRequestDto request = createDispatchRequest();
        AiMessage pending = pendingAiMessage();

        when(geminiClient.buildRequestPrompt(request)).thenReturn("prompt");
        when(aiMessageWriter.savePending(request, "prompt")).thenReturn(pending);
        when(geminiClient.generateDispatchDeadline("prompt"))
                .thenThrow(new BusinessException(MessageErrorCode.AI_CALL_FAILED));
        when(aiMessageWriter.markFailed(eq(pending.getAiMessageId()), anyString())).thenReturn(failedAiMessage());

        ApiResponse<AiDispatchDeadlineResponseDto> response =
                aiService.dispatchDeadline(request, UUID.randomUUID(), UserRole.SUPPLIER_MANAGER, "delivery-service");

        assertThat(response.isSuccess()).isTrue();
    }

    // ── A4 재생성 ────────────────────────────────────────────

    @Test
    void retryAiMessage_alreadySucceeded_propagatesConflict() {
        UUID aiMessageId = UUID.randomUUID();
        when(aiMessageWriter.increaseRetry(aiMessageId))
                .thenThrow(new BusinessException(MessageErrorCode.AI_MESSAGE_ALREADY_SUCCEEDED));

        assertThatThrownBy(() -> aiService.retryAiMessage(aiMessageId, null, UUID.randomUUID(), UserRole.MASTER, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.AI_MESSAGE_ALREADY_SUCCEEDED));
        verify(geminiClient, never()).generateDispatchDeadline(anyString());
    }

    @Test
    void retryAiMessage_success_resendsSlackByDefault() {
        UUID aiMessageId = UUID.randomUUID();
        AiMessage retried = pendingAiMessage();
        AiMessage succeeded = succeededAiMessage();
        DispatchDeadlineAiResult aiResult = new DispatchDeadlineAiResult(LocalDateTime.now().plusDays(1), "재발송 메시지");

        when(aiMessageWriter.increaseRetry(aiMessageId)).thenReturn(retried);
        when(geminiClient.generateDispatchDeadline(retried.getRequestPrompt())).thenReturn(aiResult);
        when(geminiClient.getModelName()).thenReturn("gemini-flash-latest");
        when(aiMessageWriter.markSuccess(eq(aiMessageId), anyString(), any(), anyString())).thenReturn(succeeded);
        when(slackSender.send(any(SlackSendCommand.class))).thenReturn(SlackMessageCreateResponseDto.builder()
                .slackMessageId(UUID.randomUUID())
                .status(SlackSendStatus.SUCCESS)
                .build());

        AiMessageRetryResponseDto response =
                aiService.retryAiMessage(aiMessageId, null, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.getStatus()).isEqualTo(AiCallStatus.SUCCESS);
        verify(slackSender).send(any(SlackSendCommand.class));
    }

    @Test
    void retryAiMessage_resendSlackFalse_skipsSlackSend() {
        UUID aiMessageId = UUID.randomUUID();
        AiMessage retried = pendingAiMessage();
        AiMessage succeeded = succeededAiMessage();
        DispatchDeadlineAiResult aiResult = new DispatchDeadlineAiResult(LocalDateTime.now().plusDays(1), "메시지");
        AiMessageRetryRequestDto request = AiMessageRetryRequestDto.builder().resendSlack(false).build();

        when(aiMessageWriter.increaseRetry(aiMessageId)).thenReturn(retried);
        when(geminiClient.generateDispatchDeadline(retried.getRequestPrompt())).thenReturn(aiResult);
        when(geminiClient.getModelName()).thenReturn("gemini-flash-latest");
        when(aiMessageWriter.markSuccess(eq(aiMessageId), anyString(), any(), anyString())).thenReturn(succeeded);

        aiService.retryAiMessage(aiMessageId, request, UUID.randomUUID(), UserRole.MASTER, null);

        verify(slackSender, never()).send(any());
    }

    // ── A5 이력 수정 ────────────────────────────────────────────

    @Test
    void updateAiMessage_responseReflectsEntityNotRawRequest() {
        UUID aiMessageId = UUID.randomUUID();
        AiMessage existing = succeededAiMessage();
        AiMessageUpdateRequestDto request = AiMessageUpdateRequestDto.builder()
                .failReason("수동 정정 사유")
                .build();

        when(aiMessageRepository.findByAiMessageIdAndDeletedAtIsNull(aiMessageId)).thenReturn(Optional.of(existing));

        AiMessageUpdateResponseDto response =
                aiService.updateAiMessage(aiMessageId, request, UUID.randomUUID(), UserRole.MASTER);

        // status/finalDispatchDeadline 을 요청에 넣지 않았으므로, 응답은 기존 엔티티 값(SUCCESS)을 그대로 반영해야 한다.
        assertThat(response.getStatus()).isEqualTo(AiCallStatus.SUCCESS);
    }

    // ── A7 취소 ────────────────────────────────────────────

    @Test
    void cancelDispatchDeadline_noHistory_returnsSuccessWithoutError() {
        AiCancelRequestDto request = AiCancelRequestDto.builder().orderId(UUID.randomUUID()).build();
        when(aiMessageRepository.findByOrderIdAndDeletedAtIsNull(request.getOrderId())).thenReturn(Optional.empty());

        ApiResponse<AiCancelResponseDto> response =
                aiService.cancelDispatchDeadline(request, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.isSuccess()).isTrue();
        verify(aiMessageWriter, never()).cancel(any());
    }

    @Test
    void cancelDispatchDeadline_pendingHistory_cancelsWithoutSlackNotice() {
        AiCancelRequestDto request = AiCancelRequestDto.builder().orderId(UUID.randomUUID()).build();
        AiMessage pending = pendingAiMessage();
        AiMessage cancelled = pendingAiMessage();
        cancelled.cancel();

        when(aiMessageRepository.findByOrderIdAndDeletedAtIsNull(request.getOrderId())).thenReturn(Optional.of(pending));
        when(aiMessageWriter.cancel(pending.getAiMessageId())).thenReturn(cancelled);

        ApiResponse<AiCancelResponseDto> response =
                aiService.cancelDispatchDeadline(request, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.getData().getStatus()).isEqualTo(AiCallStatus.CANCELLED);
        verify(slackSender, never()).send(any());
    }

    @Test
    void cancelDispatchDeadline_succeededHistory_sendsCancelNoticeWithoutChangingStatus() {
        AiCancelRequestDto request = AiCancelRequestDto.builder().orderId(UUID.randomUUID()).build();
        AiMessage succeeded = succeededAiMessage();

        when(aiMessageRepository.findByOrderIdAndDeletedAtIsNull(request.getOrderId())).thenReturn(Optional.of(succeeded));
        when(slackSender.send(any(SlackSendCommand.class))).thenReturn(SlackMessageCreateResponseDto.builder()
                .slackMessageId(UUID.randomUUID())
                .status(SlackSendStatus.SUCCESS)
                .build());

        ApiResponse<AiCancelResponseDto> response =
                aiService.cancelDispatchDeadline(request, UUID.randomUUID(), UserRole.MASTER, null);

        assertThat(response.getData().getStatus()).isEqualTo(AiCallStatus.SUCCESS);
        verify(aiMessageWriter, never()).cancel(any());
        verify(slackSender).send(any(SlackSendCommand.class));
    }

    // ── 헬퍼 ────────────────────────────────────────────

    private AiDispatchDeadlineRequestDto createDispatchRequest() {
        return AiDispatchDeadlineRequestDto.builder()
                .orderId(UUID.randomUUID())
                .deliveryId(UUID.randomUUID())
                .productInfo("마른 오징어 50개")
                .originHubName("경기 북부 센터")
                .destAddress("부산시 사하구 낙동대로 1번길 1")
                .managerSlackId("U0987ZZZ")
                .build();
    }

    private AiMessage pendingAiMessage() {
        return AiMessage.builder()
                .aiMessageId(UUID.randomUUID())
                .deliveryId(UUID.randomUUID())
                .managerSlackId("U0987ZZZ")
                .requestPrompt("prompt")
                .build();
    }

    private AiMessage succeededAiMessage() {
        AiMessage aiMessage = pendingAiMessage();
        aiMessage.markSuccess("응답 내용", LocalDateTime.now().plusDays(1), "gemini-flash-latest");
        return aiMessage;
    }

    private AiMessage failedAiMessage() {
        AiMessage aiMessage = pendingAiMessage();
        aiMessage.markFailed("실패 사유");
        return aiMessage;
    }
}
