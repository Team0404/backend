package com.sparta.slack.ai.client;

import com.sparta.common.exception.BusinessException;
import com.sparta.slack.ai.client.dto.DispatchDeadlineAiResult;
import com.sparta.slack.ai.domain.dto.request.AiDispatchDeadlineRequestDto;
import com.sparta.slack.common.exception.MessageErrorCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini(Spring AI) 호출 담당. {@link #generateDispatchDeadline} 의 반환값이
 * DB 적재/슬랙 발송의 입력이 되므로, 이 클래스는 순수하게 "프롬프트 → 구조화 응답" 변환만 한다.
 * 호출 결과를 {@code AiMessage} 에 반영하는 것은 {@link com.sparta.slack.ai.service.AiMessageWriter} 의 몫이다.
 *
 * <p><b>재시도 1단계(즉시 재시도).</b> {@link TransientAiException}(429/5xx 등 Gemini 쪽 일시 오류)과
 * 네트워크 연결 실패만 지수 백오프로 3회까지 즉시 재시도한다. 인증 실패·잘못된 요청처럼 재시도해도
 * 결과가 같은 오류는 재시도 대상에서 제외하고 즉시 {@link MessageErrorCode#AI_CALL_FAILED} 로 변환한다.
 *
 * <p>여기서 소진된 재시도는 <b>DB의 retry_count 에 기록하지 않는다.</b>
 * retry_count 는 A4(재생성 API) 호출 횟수만 세는 2단계 재시도용 카운터다.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String DEFAULT_WORKING_HOURS = "09-18";
    private static final String NO_WAYPOINTS = "경유지 없음";
    private static final String NO_REQUEST_NOTE = "특이 요청사항 없음";

    private final ChatClient chatClient;
    private final Resource dispatchDeadlinePromptResource;
    @Getter
    private final String modelName;

    public GeminiClient(
            ChatClient dispatchDeadlineChatClient,
            @Value("classpath:prompts/dispatch-deadline.st") Resource dispatchDeadlinePromptResource,
            @Value("${spring.ai.google.genai.chat.options.model}") String modelName
    ) {
        this.chatClient = dispatchDeadlineChatClient;
        this.dispatchDeadlinePromptResource = dispatchDeadlinePromptResource;
        this.modelName = modelName;
    }

    /**
     * AiMessage.request_prompt 에 저장할 프롬프트 원문을 미리 만든다.
     * PENDING 저장(호출 전) 시점과 실제 Gemini 호출 시점에 같은 문자열을 재사용해
     * 템플릿을 두 번 렌더링하지 않는다.
     */
    public String buildRequestPrompt(AiDispatchDeadlineRequestDto request) {
        PromptTemplate template = new PromptTemplate(dispatchDeadlinePromptResource);
        return template.render(toTemplateParams(request));
    }

    @Retryable(
            retryFor = {
                    TransientAiException.class,
                    ResourceAccessException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public DispatchDeadlineAiResult generateDispatchDeadline(String requestPrompt) {
        try {
            DispatchDeadlineAiResult result = chatClient.prompt()
                    .user(requestPrompt)
                    .call()
                    .entity(DispatchDeadlineAiResult.class);

            if (result == null || result.finalDispatchDeadline() == null) {
                throw new BusinessException(MessageErrorCode.AI_RESPONSE_PARSE_FAILED);
            }

            return result;
        } catch (TransientAiException | ResourceAccessException e) {
            // @Retryable 이 처리하도록 그대로 전파한다.
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 호출 실패. requestPrompt={}", requestPrompt, e);
            throw new BusinessException(MessageErrorCode.AI_CALL_FAILED, e.getMessage());
        }
    }

    private Map<String, Object> toTemplateParams(AiDispatchDeadlineRequestDto request) {
        Map<String, Object> params = new HashMap<>();
        params.put("productInfo", request.getProductInfo());
        params.put("requestNote", blankToDefault(request.getRequestNote(), NO_REQUEST_NOTE));
        params.put("originHubName", request.getOriginHubName());
        params.put("waypointHubNames", joinOrDefault(request.getWaypointHubNames()));
        params.put("destAddress", request.getDestAddress());
        params.put("workingHours", blankToDefault(request.getWorkingHours(), DEFAULT_WORKING_HOURS));
        return params;
    }

    private String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private String joinOrDefault(List<String> values) {
        if (values == null || values.isEmpty()) {
            return NO_WAYPOINTS;
        }
        return String.join(", ", values);
    }
}
