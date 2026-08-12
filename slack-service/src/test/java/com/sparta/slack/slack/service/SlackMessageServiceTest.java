package com.sparta.slack.slack.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.slack.common.exception.MessageErrorCode;
import com.sparta.slack.slack.client.SlackWebhookClient;
import com.sparta.slack.slack.domain.dto.command.SlackSendCommand;
import com.sparta.slack.slack.domain.dto.request.SlackMessageCreateRequestDto;
import com.sparta.slack.slack.domain.dto.request.SlackMessageUpdateRequestDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageUpdateResponseDto;
import com.sparta.slack.slack.domain.entity.SlackMessage;
import com.sparta.slack.slack.domain.entity.SlackSendStatus;
import com.sparta.slack.slack.repository.SlackMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackMessageServiceTest {

    private SlackMessageWriter slackMessageWriter;
    private SlackWebhookClient slackWebhookClient;
    private SlackMessageRepository slackMessageRepository;
    private SlackMessageService slackMessageService;

    @BeforeEach
    void setUp() {
        slackMessageWriter = mock(SlackMessageWriter.class);
        slackWebhookClient = mock(SlackWebhookClient.class);
        slackMessageRepository = mock(SlackMessageRepository.class);
        slackMessageService = new SlackMessageService(slackMessageWriter, slackWebhookClient, slackMessageRepository);
    }

    // ── M1 발송 ────────────────────────────────────────────

    @Test
    void createSlackMessage_webhookSuccess_marksSuccess() {
        SlackMessageCreateRequestDto request = SlackMessageCreateRequestDto.builder()
                .receiverSlackId("U0123ABC")
                .message("메시지")
                .build();
        SlackMessage pending = pendingSlackMessage();
        SlackMessage succeeded = succeededSlackMessage();

        when(slackMessageWriter.savePending(any(SlackSendCommand.class))).thenReturn(pending);
        when(slackMessageWriter.markSuccess(pending.getSlackMessageId())).thenReturn(succeeded);

        ApiResponse<SlackMessageCreateResponseDto> response = slackMessageService.createSlackMessage(request);

        assertThat(response.getData().getStatus()).isEqualTo(SlackSendStatus.SUCCESS);
    }

    @Test
    void createSlackMessage_webhookFails_marksFailedWithoutThrowing() {
        SlackMessageCreateRequestDto request = SlackMessageCreateRequestDto.builder()
                .receiverSlackId("U0123ABC")
                .message("메시지")
                .build();
        SlackMessage pending = pendingSlackMessage();
        SlackMessage failed = failedSlackMessage();

        when(slackMessageWriter.savePending(any(SlackSendCommand.class))).thenReturn(pending);
        doThrow(new RuntimeException("Webhook 타임아웃"))
                .when(slackWebhookClient).send(anyString(), anyString());
        when(slackMessageWriter.markFailed(eq(pending.getSlackMessageId()), anyString())).thenReturn(failed);

        ApiResponse<SlackMessageCreateResponseDto> response = slackMessageService.createSlackMessage(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(SlackSendStatus.FAILED);
    }

    // ── M2 재발송 ────────────────────────────────────────────

    @Test
    void resendSlackMessage_deniedForNonMaster() {
        UUID slackMessageId = UUID.randomUUID();

        assertThatThrownBy(() -> slackMessageService.resendSlackMessage(slackMessageId, UserRole.HUB_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        verify(slackMessageWriter, never()).increaseRetry(any());
    }

    @Test
    void resendSlackMessage_alreadySent_propagatesConflict() {
        UUID slackMessageId = UUID.randomUUID();
        when(slackMessageWriter.increaseRetry(slackMessageId))
                .thenThrow(new BusinessException(MessageErrorCode.SLACK_MESSAGE_ALREADY_SENT));

        assertThatThrownBy(() -> slackMessageService.resendSlackMessage(slackMessageId, UserRole.MASTER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.SLACK_MESSAGE_ALREADY_SENT));
        verify(slackWebhookClient, never()).send(anyString(), anyString());
    }

    @Test
    void resendSlackMessage_updatesSameHistoryRow_doesNotCreateNewOne() {
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage retried = pendingSlackMessage();
        SlackMessage succeeded = succeededSlackMessage();

        when(slackMessageWriter.increaseRetry(slackMessageId)).thenReturn(retried);
        when(slackMessageWriter.markSuccess(slackMessageId)).thenReturn(succeeded);

        ApiResponse<SlackMessageCreateResponseDto> response =
                slackMessageService.resendSlackMessage(slackMessageId, UserRole.MASTER);

        assertThat(response.getData().getStatus()).isEqualTo(SlackSendStatus.SUCCESS);
        // 재발송은 새 이력을 만들지 않고, 같은 slackMessageId 에 결과만 갱신해야 한다.
        verify(slackMessageWriter, never()).savePending(any());
        verify(slackMessageWriter).markSuccess(slackMessageId);
    }

    @Test
    void resendSlackMessage_webhookFails_marksFailedOnSameRow() {
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage retried = pendingSlackMessage();
        SlackMessage failed = failedSlackMessage();

        when(slackMessageWriter.increaseRetry(slackMessageId)).thenReturn(retried);
        doThrow(new RuntimeException("Webhook 오류"))
                .when(slackWebhookClient).send(anyString(), anyString());
        when(slackMessageWriter.markFailed(eq(slackMessageId), anyString())).thenReturn(failed);

        ApiResponse<SlackMessageCreateResponseDto> response =
                slackMessageService.resendSlackMessage(slackMessageId, UserRole.MASTER);

        assertThat(response.getData().getStatus()).isEqualTo(SlackSendStatus.FAILED);
        verify(slackMessageWriter, never()).savePending(any());
    }

    // ── M5 이력 수정 ────────────────────────────────────────────

    @Test
    void updateSlackMessage_responseReflectsEntityNotRawRequest() {
        UUID slackMessageId = UUID.randomUUID();
        SlackMessage existing = succeededSlackMessage();
        SlackMessageUpdateRequestDto request = SlackMessageUpdateRequestDto.builder()
                .message("정정된 내용")
                .build();

        when(slackMessageRepository.findBySlackMessageIdAndDeletedAtIsNull(slackMessageId))
                .thenReturn(Optional.of(existing));

        SlackMessageUpdateResponseDto response =
                slackMessageService.updateSlackMessage(slackMessageId, request, UserRole.MASTER);

        // status 를 요청에 넣지 않았으므로, 응답은 기존 엔티티 값(SUCCESS)을 그대로 반영해야 한다.
        assertThat(response.getStatus()).isEqualTo(SlackSendStatus.SUCCESS);
    }

    @Test
    void updateSlackMessage_deniedForNonMaster() {
        UUID slackMessageId = UUID.randomUUID();
        SlackMessageUpdateRequestDto request = SlackMessageUpdateRequestDto.builder().build();

        assertThatThrownBy(() -> slackMessageService.updateSlackMessage(slackMessageId, request, UserRole.DELIVERY_MANAGER))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
    }

    // ── M6 삭제 ────────────────────────────────────────────

    @Test
    void deleteSlackMessage_masterSuccess_softDeletes() {
        UUID slackMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SlackMessage existing = succeededSlackMessage();

        when(slackMessageRepository.findBySlackMessageIdAndDeletedAtIsNull(slackMessageId))
                .thenReturn(Optional.of(existing));

        ApiResponse<Void> response = slackMessageService.deleteSlackMessage(slackMessageId, userId, UserRole.MASTER);

        assertThat(response.isSuccess()).isTrue();
        assertThat(existing.isDeleted()).isTrue();
    }

    // ── 헬퍼 ────────────────────────────────────────────

    private SlackMessage pendingSlackMessage() {
        return SlackMessage.builder()
                .slackMessageId(UUID.randomUUID())
                .receiverSlackId("U0123ABC")
                .message("메시지")
                .build();
    }

    private SlackMessage succeededSlackMessage() {
        SlackMessage slackMessage = pendingSlackMessage();
        slackMessage.markSuccess();
        return slackMessage;
    }

    private SlackMessage failedSlackMessage() {
        SlackMessage slackMessage = pendingSlackMessage();
        slackMessage.markFailed("실패 사유");
        return slackMessage;
    }
}
