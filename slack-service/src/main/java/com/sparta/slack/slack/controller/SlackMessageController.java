package com.sparta.slack.slack.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.slack.slack.domain.dto.request.SlackMessageCreateRequestDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.service.SlackMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 슬랙 메시지(M1~M6) API.
 *
 * 인증/인가는 Gateway가 JWT 검증 후 전달하는 헤더에 의존한다.
 */
@Tag(name = "SlackMessage", description = "슬랙 메시지 발송 및 이력 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/slack-messages")
public class SlackMessageController {

    private final SlackMessageService slackMessageService;

    @Operation(
            summary = "슬랙 메시지 생성·발송",
            description = "슬랙으로 메시지를 발송하고 이력을 저장합니다. 발송 실패 시 status=FAILED로 저장되어 "
                    + "재발송(M2) 대상이 됩니다. 허용 권한: 로그인한 모든 사용자 및 내부 시스템"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SlackMessageCreateResponseDto>> createSlackMessage(
            @RequestBody @Valid SlackMessageCreateRequestDto request
    ) {
        ApiResponse<SlackMessageCreateResponseDto> response = slackMessageService.createSlackMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
