package com.sparta.slack.slack.domain.dto.command;

import com.sparta.slack.ai.domain.entity.AiMessage;
import com.sparta.slack.slack.domain.entity.SlackMessageType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 서비스 내부에서 슬랙 발송을 요청할 때 쓰는 커맨드.
 *
 * <p>웹 요청 DTO(M1)와 분리해 둔 이유는, AI(A1/A4)처럼 HTTP 요청 없이
 * 내부에서 발송을 트리거하는 경로가 있기 때문이다.
 */
@Getter
@Builder
public class SlackSendCommand {

    private String receiverSlackId;

    private String message;

    @Builder.Default
    private SlackMessageType messageType = SlackMessageType.GENERAL;

    /** 관련 주문/배송 ID */
    private UUID referenceId;

    /** AI가 생성한 메시지인 경우 연결할 AI 로그. 일반 발송(M1)이면 null */
    private AiMessage aiMessage;
}
