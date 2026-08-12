package com.sparta.slack.common.exception;

import com.sparta.common.exception.ErrorCodeIfs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 메시지 서비스(슬랙 M1~M6 / AI A1~A6) 전용 에러 코드.
 * 범용 코드(인증/인가 실패, 입력값 검증 등)는 {@link com.sparta.common.exception.ErrorCode}를 사용한다.
 *
 * <p>슬랙 담당자와 AI 담당자가 함께 쓰는 파일이므로, 개발 중 서로 충돌하지 않도록
 * 필요한 코드를 미리 모두 정의해 둔다. 추가가 필요하면 각자 구역(M1xx / M2xx)의
 * 마지막 번호 뒤에만 덧붙인다.
 */
@Getter
@RequiredArgsConstructor
public enum MessageErrorCode implements ErrorCodeIfs {

    // --- 슬랙 메시지 (M1xx) ---
    SLACK_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "M101", "존재하지 않는 슬랙 메시지 이력입니다."),
    SLACK_MESSAGE_ALREADY_SENT(HttpStatus.CONFLICT, "M102", "이미 발송에 성공한 슬랙 메시지입니다."),
    SLACK_RETRY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "M103", "슬랙 재발송 한도를 초과했습니다."),
    SLACK_SEND_FAILED(HttpStatus.BAD_GATEWAY, "M104", "슬랙 메시지 발송에 실패했습니다."),
    SLACK_WEBHOOK_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "M105", "슬랙 Webhook 설정이 존재하지 않습니다."),
    SLACK_MESSAGE_ALREADY_DELETED(HttpStatus.CONFLICT, "M106", "이미 삭제된 슬랙 메시지 이력입니다."),

    // --- AI 메시지 (M2xx) ---
    AI_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "M201", "존재하지 않는 AI 메시지 이력입니다."),
    AI_MESSAGE_ALREADY_SUCCEEDED(HttpStatus.CONFLICT, "M202", "이미 호출에 성공한 AI 메시지입니다."),
    AI_RETRY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "M203", "AI 재생성 한도를 초과했습니다."),
    AI_CALL_FAILED(HttpStatus.BAD_GATEWAY, "M204", "AI 호출에 실패했습니다."),
    AI_RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M205", "AI 응답에서 최종 발송 시한을 파싱하지 못했습니다."),
    AI_MESSAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "M206", "이미 발송 시한이 생성된 주문입니다."),
    AI_API_KEY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "M207", "AI API 키 설정이 존재하지 않습니다."),
    AI_MESSAGE_ALREADY_DELETED(HttpStatus.CONFLICT, "M208", "이미 삭제된 AI 메시지 이력입니다."),
    AI_MESSAGE_CANCELLED(HttpStatus.CONFLICT, "M209", "주문 취소로 무효화된 AI 메시지입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
