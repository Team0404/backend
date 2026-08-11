package com.sparta.slack.slack.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * M1. 슬랙 메시지 생성·발송 요청.
 * 로그인한 모든 사용자 및 내부 시스템이 호출할 수 있다.
 */
@Getter
@Builder
public class SlackMessageCreateRequestDto {

    @NotNull
    @NotBlank
    private String receiverSlackId;

    @NotNull
    @NotBlank
    private String message;

    private String messageType;

    private UUID referenceId;
}
