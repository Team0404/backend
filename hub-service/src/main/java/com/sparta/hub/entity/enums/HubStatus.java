package com.sparta.hub.entity.enums;

/*
* ACTIVE
→ 삭제 요청
→ DEACTIVATING
→ 진행 중인 배송이 모두 종료
→ INACTIVE + Soft Delete
* */

public enum HubStatus {
    ACTIVE,         // 신규 배송 경로 배정 가능
    DEACTIVATING,   // 신규 배송 차단, 기존 배송 처리 중
    INACTIVE        // 기존 배송 종료, 허브 운영 중지
}
