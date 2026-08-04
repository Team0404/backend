package com.sparta.delivery.domain.entity;

/**
 * 배송 담당자 타입.
 *  - HUB     : 허브 간 이동 담당 (전체 풀, 소속 허브 없음 → hub_id NULL)
 *  - COMPANY : 최종 허브 → 수령 업체 이동 담당 (허브별 풀 → hub_id 필수)
 */
public enum DeliveryManagerType {
    HUB,
    COMPANY
}
