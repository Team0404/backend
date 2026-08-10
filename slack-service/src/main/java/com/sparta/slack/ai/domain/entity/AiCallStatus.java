package com.sparta.slack.ai.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;

/**
 * AI 호출 상태.
 *  - PENDING : 호출 대기/진행 중
 *  - SUCCESS : 호출 성공
 *  - FAILED  : 호출 실패(재생성 대상, 한도 초과 시 최종 실패)
 */
public enum AiCallStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public static AiCallStatus fromString(String status) {
        try {
            return AiCallStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 status 입니다.");
        }
    }
}
