package com.sparta.slack.slack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 슬랙 발송 설정. 값은 {@code .env} → 환경변수 → application.yml 순으로 주입된다.
 *
 * <p>webhookUrl 이 비어 있어도 애플리케이션은 정상 기동한다. 팀원이 슬랙 키 없이도
 * 다른 기능을 개발할 수 있어야 하기 때문이며, 실제 발송 시점에만
 * {@code SLACK_WEBHOOK_NOT_CONFIGURED} 로 실패 처리한다.
 */
@ConfigurationProperties(prefix = "slack")
public record SlackProperties(
        String token,
        String webhookUrl
) {
    public boolean hasWebhookUrl() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }
}
