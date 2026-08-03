package com.sparta.common.entity;

/**
 * 시스템 전역 사용자 권한.
 * 게이트웨이에서 토큰 검증 후 각 서비스로 전달되는 권한 값과 동일하게 사용한다.
 */
public enum UserRole {
    MASTER,             // 마스터 관리자
    HUB_MANAGER,        // 허브 관리자
    DELIVERY_MANAGER,   // 배송 담당자
    SUPPLIER_MANAGER    // 업체 담당자
}
