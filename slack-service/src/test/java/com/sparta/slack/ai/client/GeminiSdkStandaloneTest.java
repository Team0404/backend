package com.sparta.slack.ai.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import org.junit.jupiter.api.Test;

/**
 * 임시 진단용. Spring을 완전히 배제하고 SDK만 단독으로 호출해서,
 * "REST curl은 되는데 SDK는 안 되는" 증상이 SDK 자체 문제인지 Spring 배선 문제인지 가른다.
 * 확인 끝나면 삭제.
 */
class GeminiSdkStandaloneTest {

    @Test
    void callDirectlyWithoutSpring() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 환경변수를 실행 전에 설정해줘 (Run Configuration 아님, 이 테스트는 별도 실행)");
        }

        Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(30_000).build())
                .build();

        GenerateContentResponse response = client.models.generateContent(
                "gemini-flash-latest",
                "hello",
                null
        );

        System.out.println("=== SDK 단독 호출 결과 ===");
        System.out.println(response.text());
    }
}
