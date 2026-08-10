package com.sparta.slack.slack.client.dto;

/**
 * Incoming Webhook 요청 본문.
 *
 * @see <a href="https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/">Slack Incoming Webhooks</a>
 */
public record SlackWebhookPayload(String text) {
}
