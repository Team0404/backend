package com.sparta.slack.slack.client.dto;

/**
 * Incoming Webhook 요청 본문.
 */
public record SlackWebhookPayload(String text) {
}
