package com.sparta.slack.domain.entity;

/**
 * 슬랙 메시지 타입.
 *  - GENERAL           : 일반 메시지
 *  - DISPATCH_DEADLINE : 발송 시한 알림(AI 산출)
 *  - DAILY_ROUTE       : 아침 6시 경로 알림(도전 기능)
 */
public enum SlackMessageType {
    GENERAL,
    DISPATCH_DEADLINE,
    DAILY_ROUTE
}
