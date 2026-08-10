package com.sparta.slack.ai.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;

/**
 * AI 호출 상태.
 *  - PENDING   : 호출 대기/진행 중
 *  - SUCCESS   : 호출 성공
 *  - FAILED    : 호출 실패(재생성 대상, 한도 초과 시 최종 실패)
 *  - CANCELLED : 주문 취소로 무효화됨. 슬랙 발송이 억제되며 재생성 대상에서 제외된다.
 *
 * <p>CANCELLED 는 Soft Delete(deleted_at) 와 구분된다. 삭제는 이력 자체를 조회 대상에서
 * 빼는 것이고, 취소는 "이력은 남기되 후속 발송을 막는" 상태이므로 조회에는 계속 잡혀야 한다.
 */
public enum AiCallStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED;

    public static AiCallStatus fromString(String status) {
        try {
            return AiCallStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 status 입니다.");
        }
    }
}
