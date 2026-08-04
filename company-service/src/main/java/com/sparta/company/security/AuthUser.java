package com.sparta.company.security;

import java.util.UUID;

/**
 * Gateway에서 JWT 검증 후 내려주는 X-User-Id / X-Username / X-User-Role 헤더를 담는 값 객체.
 * userId는 프로젝트 전체 컨벤션(UUID PK)에 맞춰 UUID로 파싱해서 보관한다.
 */
public record AuthUser(
        UUID userId,
        String username,
        String role
) {

    public boolean isMaster() {
        return "MASTER".equals(role);
    }

    public boolean isHubManager() {
        return "HUB_MANAGER".equals(role);
    }

    public boolean isDeliveryManager() {
        return "DELIVERY_MANAGER".equals(role);
    }

    public boolean isSupplierManager() {
        return "SUPPLIER_MANAGER".equals(role);
    }
}
