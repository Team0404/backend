package com.sparta.slack.slack.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;

/**
 * 슬랙 메시지 발송 상태.
 *  - PENDING : 발송 대기
 *  - SUCCESS : 발송 성공
 *  - FAILED  : 발송 실패(재발송 대상, 한도 초과 시 최종 실패)
 */
public enum SlackSendStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public static SlackSendStatus fromString(String status) {
        try {
            return SlackSendStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 status 입니다.");
        }
    }
}
