package com.sparta.slack.ai.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.slack.common.exception.MessageErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiMessageTest {

    private AiMessage newPending() {
        return AiMessage.builder()
                .requestPrompt("prompt")
                .managerSlackId("U0987ZZZ")
                .build();
    }

    @Test
    void markSuccess_setsSuccessStatusAndClearsFailReason() {
        AiMessage aiMessage = newPending();
        aiMessage.markFailed("이전 실패 사유");

        aiMessage.markSuccess("응답 내용", LocalDateTime.now().plusDays(1), "gemini-flash-latest");

        assertThat(aiMessage.getStatus()).isEqualTo(AiCallStatus.SUCCESS);
        assertThat(aiMessage.getFailReason()).isNull();
        assertThat(aiMessage.getResponseContent()).isEqualTo("응답 내용");
    }

    @Test
    void markFailed_setsFailedStatusAndFailReason() {
        AiMessage aiMessage = newPending();

        aiMessage.markFailed("타임아웃");

        assertThat(aiMessage.getStatus()).isEqualTo(AiCallStatus.FAILED);
        assertThat(aiMessage.getFailReason()).isEqualTo("타임아웃");
    }

    @Test
    void cancel_pendingOrFailed_transitionsToCancelled() {
        AiMessage pending = newPending();
        pending.cancel();
        assertThat(pending.getStatus()).isEqualTo(AiCallStatus.CANCELLED);

        AiMessage failed = newPending();
        failed.markFailed("사유");
        failed.cancel();
        assertThat(failed.getStatus()).isEqualTo(AiCallStatus.CANCELLED);
    }

    @Test
    void cancel_success_doesNotChangeStatus() {
        AiMessage succeeded = newPending();
        succeeded.markSuccess("내용", LocalDateTime.now().plusDays(1), "model");

        succeeded.cancel();

        assertThat(succeeded.getStatus()).isEqualTo(AiCallStatus.SUCCESS);
    }

    @Test
    void increaseRetry_alreadySucceeded_throwsConflict() {
        AiMessage succeeded = newPending();
        succeeded.markSuccess("내용", LocalDateTime.now().plusDays(1), "model");

        assertThatThrownBy(succeeded::increaseRetry)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.AI_MESSAGE_ALREADY_SUCCEEDED));
    }

    @Test
    void increaseRetry_cancelled_throwsConflict() {
        AiMessage cancelled = newPending();
        cancelled.cancel();

        assertThatThrownBy(cancelled::increaseRetry)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.AI_MESSAGE_CANCELLED));
    }

    @Test
    void increaseRetry_limitExceeded_throwsConflict() {
        AiMessage aiMessage = AiMessage.builder()
                .requestPrompt("prompt")
                .managerSlackId("U0987ZZZ")
                .retryCount(3)
                .maxRetryCount(3)
                .build();

        assertThatThrownBy(aiMessage::increaseRetry)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.AI_RETRY_LIMIT_EXCEEDED));
    }

    @Test
    void increaseRetry_withinLimit_incrementsCount() {
        AiMessage aiMessage = newPending();
        aiMessage.markFailed("사유");

        aiMessage.increaseRetry();

        assertThat(aiMessage.getRetryCount()).isEqualTo(1);
    }

    @Test
    void update_partialFields_keepsUntouchedFieldsAsIs() {
        AiMessage aiMessage = newPending();
        aiMessage.markSuccess("원래 응답", LocalDateTime.now().plusDays(1), "model");

        aiMessage.update("FAILED", null, null, "정정된 실패 사유");

        assertThat(aiMessage.getStatus()).isEqualTo(AiCallStatus.FAILED);
        assertThat(aiMessage.getFailReason()).isEqualTo("정정된 실패 사유");
        // 넘기지 않은 필드는 그대로 유지된다.
        assertThat(aiMessage.getResponseContent()).isEqualTo("원래 응답");
    }
}
