package com.sparta.slack.slack.client;

import com.sparta.common.exception.BusinessException;
import com.sparta.slack.common.exception.MessageErrorCode;
import com.sparta.slack.slack.client.dto.SlackWebhookPayload;
import com.sparta.slack.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 슬랙 Incoming Webhook 호출 담당.
 *
 * <p><b>재시도 1단계(즉시 재시도).</b> 네트워크 오류·5xx·429 같은 일시 오류만
 * 지수 백오프로 3회까지 즉시 재시도한다. 4xx(잘못된 payload, 폐기된 webhook 등)는
 * 재시도해도 결과가 같으므로 즉시 전파한다.
 *
 * <p>여기서 소진된 재시도는 <b>DB의 retry_count 에 기록하지 않는다.</b>
 * retry_count 는 M2(재발송 API) 호출 횟수만 세는 2단계 재시도용 카운터다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackWebhookClient {

    private final RestClient slackRestClient;
    private final SlackProperties slackProperties;

    @Retryable(
            retryFor = {
                    ResourceAccessException.class,          // 연결 실패 / 타임아웃
                    HttpServerErrorException.class,         // 5xx
                    HttpClientErrorException.TooManyRequests.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void send(String receiverSlackId, String message) {
        if (!slackProperties.hasWebhookUrl()) {
            throw new BusinessException(MessageErrorCode.SLACK_WEBHOOK_NOT_CONFIGURED);
        }

        slackRestClient.post()
                .uri(slackProperties.webhookUrl())
                .body(new SlackWebhookPayload(toText(receiverSlackId, message)))
                .retrieve()
                .toBodilessEntity();

        log.debug("슬랙 발송 성공. receiverSlackId={}", receiverSlackId);
    }

    /**
     * Incoming Webhook 은 설정된 채널로만 발송되고 수신자를 지정할 수 없다.
     * 따라서 본문 앞에 멘션을 붙여 수신 대상을 표시한다.
     */
    private String toText(String receiverSlackId, String message) {
        if (receiverSlackId == null || receiverSlackId.isBlank()) {
            return message;
        }
        return "<@" + receiverSlackId + "> " + message;
    }
}
