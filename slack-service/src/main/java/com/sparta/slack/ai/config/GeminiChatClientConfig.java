package com.sparta.slack.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini 호출용 {@link ChatClient} 빈 설정.
 *
 * <p>{@code ChatClient.Builder} 는 spring-ai 스타터가 classpath의 ChatModel(GenAI) 구현체를
 * 감지해 자동 구성해 준다. 여기서는 그 Builder에 발송 시한 산출 도메인에 맞는 기본 system
 * 프롬프트만 얹어 애플리케이션 전용 빈으로 노출한다.
 */
@Configuration
public class GeminiChatClientConfig {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 물류 회사의 배차 담당 보조입니다. 응답은 항상 요청된 JSON 스키마를 정확히 따르고,
            발송 시한은 절대 과거 시각으로 답하지 마세요.
            """;

    @Bean
    public ChatClient dispatchDeadlineChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }
}
