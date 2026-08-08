package com.sparta.slack.domain.entity;

/**
 * AI 호출 상태.
 *  - PENDING : 호출 대기/진행 중
 *  - SUCCESS : 호출 성공
 *  - FAILED  : 호출 실패(재생성 대상, 한도 초과 시 최종 실패)
 */
public enum AiCallStatus {
    PENDING,
    SUCCESS,
    FAILED
}
