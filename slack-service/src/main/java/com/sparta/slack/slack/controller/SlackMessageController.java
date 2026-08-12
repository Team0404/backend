package com.sparta.slack.slack.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.slack.slack.domain.dto.request.SlackMessageCreateRequestDto;
import com.sparta.slack.slack.domain.dto.request.SlackMessageUpdateRequestDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageCreateResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageFindResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageSummaryResponseDto;
import com.sparta.slack.slack.domain.dto.response.SlackMessageUpdateResponseDto;
import com.sparta.slack.slack.service.SlackMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    /** 내부망 전용이라 GateWay 내부가 보장됨. */
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

    @Operation(
            summary = "슬랙 메시지 재발송",
            description = "슬랙으로 메시지를 발송하고 이력을 저장합니다. 발송 실패 시 status=FAILED로 저장되어 "
                    + "재발송(M2) 대상이 됩니다. 허용 권한: 로그인한 모든 사용자 및 내부 시스템"
    )
    @PreAuthorize("hasRole('MASTER')")
    @PostMapping("/{slackMessageId}/resend")
    public ResponseEntity<ApiResponse<SlackMessageCreateResponseDto>> resendSlackMessage(
            @PathVariable(name = "slackMessageId") UUID slackMessageId,
            @AuthenticationPrincipal UserPrincipal authUser
    ) {
        ApiResponse<SlackMessageCreateResponseDto> response = slackMessageService.resendSlackMessage(slackMessageId, authUser.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "슬랙 메시지 목록/검색",
            description = "허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping
    public ApiResponse<PageResponse<SlackMessageSummaryResponseDto>> searchSlackMessages(
            @AuthenticationPrincipal UserPrincipal authUser,
            @RequestParam(required = false) String receiverSlackId,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.success(
                slackMessageService.searchSlackMessages(receiverSlackId, messageType, status, pageable, authUser.getRole())
        );
    }

    @Operation(
            summary = "슬랙 메시지 단건 조회",
            description = "허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/{slackMessageId}")
    public ApiResponse<SlackMessageFindResponseDto> findSlackMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @PathVariable UUID slackMessageId
    ) {
        return ApiResponse.success(slackMessageService.findSlackMessage(slackMessageId, authUser.getRole()));
    }

    @Operation(
            summary = "슬랙 메시지 이력 수정",
            description = "Incoming Webhook 방식이라 슬랙 채널의 실제 메시지는 수정되지 않고, 우리 DB의 이력만 정정됩니다. "
                    + "허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping("/{slackMessageId}")
    public ApiResponse<SlackMessageUpdateResponseDto> updateSlackMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @PathVariable UUID slackMessageId,
            @RequestBody SlackMessageUpdateRequestDto request
    ) {
        return ApiResponse.success(
                slackMessageService.updateSlackMessage(slackMessageId, request, authUser.getRole())
        );
    }

    @Operation(
            summary = "슬랙 메시지 이력 삭제",
            description = "Soft Delete 처리됩니다. 허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/{slackMessageId}")
    public ApiResponse<Void> deleteSlackMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @PathVariable UUID slackMessageId
    ) {
        return slackMessageService.deleteSlackMessage(slackMessageId, authUser.getUserId(), authUser.getRole());
    }
}
