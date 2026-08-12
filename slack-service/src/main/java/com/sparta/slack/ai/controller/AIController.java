package com.sparta.slack.ai.controller;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.slack.ai.domain.dto.request.AiCancelRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiDispatchDeadlineRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiMessageRetryRequestDto;
import com.sparta.slack.ai.domain.dto.request.AiMessageUpdateRequestDto;
import com.sparta.slack.ai.domain.dto.response.AiCancelResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiDispatchDeadlineResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageFindResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageRetryResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageSummaryResponseDto;
import com.sparta.slack.ai.domain.dto.response.AiMessageUpdateResponseDto;
import com.sparta.slack.ai.service.AIService;
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
 * AI 발송시한(A1~A7) API.
 *
 * 인증/인가는 Gateway가 JWT 검증 후 전달하는 헤더에 의존한다.
 *
 * <p>내부 호출(배송 서비스 → AI)을 허용하는 A1/A7에는 {@code @PreAuthorize}를 붙이지 않는다.
 * delivery-service는 자체 {@code FeignClientInterceptor}로 {@code X-User-*}를 전파한다.
 *
 * <p> A1(발송시한 알림)은 {@code @Async} 리스너에서 호출돼 원래 요청 스레드를 벗어나 있어
 * 그 전파 자체가 불가능하다 — 이 경로는 항상 {@code @AuthenticationPrincipal}이 null이다.
 * A7(취소)은 delivery-service의 동기 호출 경로라 principal이 존재하지만, 인가 판단은
 * 두 API 모두 서비스가 "MASTER 또는 허용된 내부 서비스"로 일관되게 처리한다.
 */
@Tag(name = "AiMessage", description = "AI 발송시한 산출 및 이력 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AIController {

    private final AIService aiService;

    @Operation(
            summary = "AI 발송시한 생성·알림",
            description = "배송 생성 후 호출되는 내부 API입니다. AI로 최종 발송 시한을 산출하고 "
                    + "발송 허브 담당자에게 슬랙으로 알립니다. 허용 권한: MASTER 또는 내부 시스템(배송/주문 서비스)"
    )
    @PostMapping("/ai/dispatch-deadline")
    public ResponseEntity<ApiResponse<AiDispatchDeadlineResponseDto>> dispatchDeadline(
            @AuthenticationPrincipal UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @RequestBody @Valid AiDispatchDeadlineRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aiService.dispatchDeadline(
                        request, authUser == null ? null : authUser.getRole(), internalCaller
                ));
    }

    @Operation(
            summary = "AI 발송시한 취소",
            description = "주문/배송 취소 시 호출되는 보상 API입니다. 아직 발송 전이면 CANCELLED로 막고, "
                    + "이미 알림이 나갔으면 취소 안내를 재발송합니다. 동일 요청이 재시도되어도 안전합니다(멱등). "
                    + "허용 권한: MASTER 또는 내부 시스템(배송/주문 서비스)"
    )
    @PostMapping("/ai/dispatch-deadline/cancel")
    public ApiResponse<AiCancelResponseDto> cancelDispatchDeadline(
            @AuthenticationPrincipal UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @RequestBody @Valid AiCancelRequestDto request
    ) {
        return aiService.cancelDispatchDeadline(
                request, authUser == null ? null : authUser.getRole(), internalCaller
        );
    }

    @Operation(
            summary = "AI 호출 실패 메시지 목록",
            description = "재시도 대상 관리를 위한 AI 호출 로그 조회입니다. 허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/ai-messages")
    public ApiResponse<PageResponse<AiMessageSummaryResponseDto>> searchAiMessages(
            @AuthenticationPrincipal UserPrincipal authUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) String model,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.success(
                aiService.searchAiMessages(status, orderId, model, pageable, authUser.getRole())
        );
    }

    @Operation(
            summary = "AI 메시지 단건 조회",
            description = "허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @GetMapping("/ai-messages/{aiMessageId}")
    public ApiResponse<AiMessageFindResponseDto> findAiMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @PathVariable UUID aiMessageId
    ) {
        return ApiResponse.success(aiService.findAiMessage(aiMessageId, authUser.getRole()));
    }

    @Operation(
            summary = "AI 메시지 재생성(실패 시 재시도)",
            description = "저장된 프롬프트로 AI를 재호출합니다. 성공 시 슬랙 재발송을 트리거할 수 있습니다. "
                    + "허용 권한: MASTER 또는 내부 시스템(재시도 스케줄러)"
    )
    @PostMapping("/ai-messages/{aiMessageId}/retry")
    public ApiResponse<AiMessageRetryResponseDto> retryAiMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @RequestHeader(value = AuthHeaders.INTERNAL_CALL, required = false) String internalCaller,
            @PathVariable UUID aiMessageId,
            @RequestBody(required = false) AiMessageRetryRequestDto request
    ) {
        return ApiResponse.success(
                aiService.retryAiMessage(
                        aiMessageId, request, authUser == null ? null : authUser.getRole(), internalCaller
                )
        );
    }

    @Operation(
            summary = "AI 메시지 이력 수정",
            description = "AI를 재호출하지 않고 저장된 로그만 정정합니다(재호출은 A4). 허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @PatchMapping("/ai-messages/{aiMessageId}")
    public ApiResponse<AiMessageUpdateResponseDto> updateAiMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @PathVariable UUID aiMessageId,
            @RequestBody AiMessageUpdateRequestDto request
    ) {
        return ApiResponse.success(
                aiService.updateAiMessage(aiMessageId, request, authUser.getRole())
        );
    }

    @Operation(
            summary = "AI 메시지 이력 삭제",
            description = "Soft Delete 처리됩니다. 허용 권한: MASTER"
    )
    @PreAuthorize("hasRole('MASTER')")
    @DeleteMapping("/ai-messages/{aiMessageId}")
    public ApiResponse<Void> deleteAiMessage(
            @AuthenticationPrincipal UserPrincipal authUser,
            @PathVariable UUID aiMessageId
    ) {
        return aiService.deleteAiMessage(aiMessageId, authUser.getUserId(), authUser.getRole());
    }
}
