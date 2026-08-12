package com.sparta.slack.ai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini 호출용 {@link ChatClient} 및 하위 {@link Client} 빈 설정.
 */
@Configuration
public class GeminiChatClientConfig {

    /**
     * Gemini 호출 타임아웃(ms).
     *
     * <p>이 값이 없으면 응답이 오지 않을 때 호출 스레드가 무한정 붙잡힌다. A1은 배송 서비스의
     * 비동기 스레드풀(크기 5)에서 호출되므로, 몇 건만 매달려도 풀이 고갈되고 이후 알림이
     * 전부 밀린다.
     */
    private static final int TIMEOUT_MS = 30_000;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 물류 회사의 배차 담당 보조입니다. 응답은 항상 요청된 JSON 스키마를 정확히 따르고,
            발송 시한은 절대 과거 시각으로 답하지 마세요.
            """;

    /** spring-ai 자동설정({@code GoogleGenAiChatAutoConfiguration})의 Client 빈을 대체한다. */
    @Bean
    public Client googleGenAiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(TIMEOUT_MS)
                        .build())
                .build();
    }

    @Bean
    public ChatClient dispatchDeadlineChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }
}
