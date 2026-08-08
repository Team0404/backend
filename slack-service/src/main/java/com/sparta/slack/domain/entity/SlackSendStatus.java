package com.sparta.slack.domain.entity;

/**
 * 슬랙 메시지 발송 상태.
 *  - PENDING : 발송 대기
 *  - SUCCESS : 발송 성공
 *  - FAILED  : 발송 실패(재발송 대상, 한도 초과 시 최종 실패)
 */
public enum SlackSendStatus {
    PENDING,
    SUCCESS,
    FAILED
}
