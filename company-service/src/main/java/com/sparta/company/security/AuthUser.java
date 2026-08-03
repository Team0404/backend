package com.sparta.company.security;

/**
 * Gateway에서 JWT 검증 후 내려주는 X-User-Id / X-Username / X-User-Role 헤더를 담는 값 객체.
 * 컨트롤러에서 @CurrentUser AuthUser user 형태로 주입받아 사용한다.
 *
 * role 값은 common.entity.UserRole과 동일한 문자열이어야 한다 (예: MASTER, HUB_MANAGER, DELIVERY_MANAGER, SUPPLIER_MANAGER).
 *
 * 허브 관리자의 담당 허브(hubId), 업체 담당자의 소속 업체(companyId)는
 * 이 서비스가 별도로 저장하지 않으므로, User 서비스가 JWT에 심어준 값을 Gateway가
 * 추가 헤더(예: X-Hub-Id, X-Company-Id)로 내려주는 경우 여기에 필드를 추가해야 한다.
 * (팀 컨벤션 확인 필요: hubId/companyId를 헤더로 내려줄지, 아니면 이 서비스가 요청마다 User 서비스에 조회할지)
 */
public record AuthUser(
        String userId,
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
