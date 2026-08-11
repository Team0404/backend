package com.sparta.slack.slack.domain.entity;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;

/**
 * 슬랙 메시지 타입.
 *  - GENERAL           : 일반 메시지
 *  - DISPATCH_DEADLINE : 발송 시한 알림(AI 산출)
 *  - DAILY_ROUTE       : 아침 6시 경로 알림(도전 기능)
 */
public enum SlackMessageType {
    GENERAL,
    DISPATCH_DEADLINE,
    DAILY_ROUTE;

    public static SlackMessageType fromString(String messageType) {
        try {
            return SlackMessageType.valueOf(messageType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지않은 messageType 입니다.");
        }
    }
}
