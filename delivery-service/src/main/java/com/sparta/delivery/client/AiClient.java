package com.sparta.delivery.client;

import com.sparta.common.feign.InternalCallInterceptor;
import com.sparta.common.response.ApiResponse;
import com.sparta.delivery.client.dto.AiCancelRequest;
import com.sparta.delivery.client.dto.AiDispatchDeadlineRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "slack-service", contextId = "aiClient", path = "/api/v1/ai",
        configuration = InternalCallInterceptor.class)
public interface AiClient {

    /** A1. AI 발송시한 산출 + 발송 허브 담당자 슬랙 알림. */
    @PostMapping("/dispatch-deadline")
    ApiResponse<Object> dispatchDeadline(@RequestBody AiDispatchDeadlineRequest request);

    /** A7. 주문/배송 취소 시 AI 발송시한 무효화(보상). */
    @PostMapping("/dispatch-deadline/cancel")
    ApiResponse<Object> cancelDispatchDeadline(@RequestBody AiCancelRequest request);
}
