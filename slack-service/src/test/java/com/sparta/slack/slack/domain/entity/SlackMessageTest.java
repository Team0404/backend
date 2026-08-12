package com.sparta.slack.slack.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.slack.common.exception.MessageErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlackMessageTest {

    private SlackMessage newPending() {
        return SlackMessage.builder()
                .receiverSlackId("U0123ABC")
                .message("메시지")
                .build();
    }

    @Test
    void markSuccess_setsSuccessStatusAndSentAtAndClearsFailReason() {
        SlackMessage slackMessage = newPending();
        slackMessage.markFailed("이전 실패 사유");

        slackMessage.markSuccess();

        assertThat(slackMessage.getStatus()).isEqualTo(SlackSendStatus.SUCCESS);
        assertThat(slackMessage.getFailReason()).isNull();
        assertThat(slackMessage.getSentAt()).isNotNull();
    }

    @Test
    void markFailed_setsFailedStatusAndFailReason() {
        SlackMessage slackMessage = newPending();

        slackMessage.markFailed("Webhook 타임아웃");

        assertThat(slackMessage.getStatus()).isEqualTo(SlackSendStatus.FAILED);
        assertThat(slackMessage.getFailReason()).isEqualTo("Webhook 타임아웃");
    }

    @Test
    void increaseRetry_alreadySent_throwsConflict() {
        SlackMessage slackMessage = newPending();
        slackMessage.markSuccess();

        assertThatThrownBy(slackMessage::increaseRetry)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.SLACK_MESSAGE_ALREADY_SENT));
    }

    @Test
    void increaseRetry_limitExceeded_throwsConflict() {
        SlackMessage slackMessage = SlackMessage.builder()
                .receiverSlackId("U0123ABC")
                .message("메시지")
                .retryCount(3)
                .maxRetryCount(3)
                .build();

        assertThatThrownBy(slackMessage::increaseRetry)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(MessageErrorCode.SLACK_RETRY_LIMIT_EXCEEDED));
    }

    @Test
    void increaseRetry_withinLimit_incrementsCount() {
        SlackMessage slackMessage = newPending();
        slackMessage.markFailed("사유");

        slackMessage.increaseRetry();

        assertThat(slackMessage.getRetryCount()).isEqualTo(1);
    }

    @Test
    void update_partialFields_keepsUntouchedFieldsAsIs() {
        SlackMessage slackMessage = newPending();

        slackMessage.update(null, "DISPATCH_DEADLINE", "FAILED", null);

        assertThat(slackMessage.getMessageType()).isEqualTo(SlackMessageType.DISPATCH_DEADLINE);
        assertThat(slackMessage.getStatus()).isEqualTo(SlackSendStatus.FAILED);
        // 넘기지 않은 필드는 그대로 유지된다.
        assertThat(slackMessage.getMessage()).isEqualTo("메시지");
    }
}
