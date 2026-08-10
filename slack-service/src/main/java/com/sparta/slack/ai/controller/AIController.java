package com.sparta.slack.ai.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.slack.ai.domain.dto.response.AiDispatchDeadlineResponseDto;
import com.sparta.slack.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class AIController {
    private final AIService aiService;

    @PostMapping("ai/dispatch-deadline")
    public ResponseEntity<ApiResponse<AiDispatchDeadlineResponseDto>> dispatchDeadline() {
        // TODO(A1): AiDispatchDeadlineRequestDto 바인딩 + AIService 연결 (작업 순서 3번)
        throw new UnsupportedOperationException("A1 미구현");
    }
}
