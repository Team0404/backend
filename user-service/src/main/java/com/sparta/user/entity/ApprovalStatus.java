package com.sparta.user.entity;

/**
 * 회원가입 승인 상태.
 * PENDING(대기) → APPROVED(승인) / REJECTED(거절)
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
