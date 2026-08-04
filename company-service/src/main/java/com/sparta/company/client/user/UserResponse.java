package com.sparta.company.client.user;

import java.util.UUID;

/**
 * User 서비스 응답 중 스코프 판단에 필요한 필드만 추출.
 * (실제 User 서비스 응답 필드명에 맞춰 수정 필요)
 */
public record UserResponse(
        UUID userId,
        String username,
        String role,
        UUID hubId,       // HUB_MANAGER인 경우 담당 허브
        UUID companyId    // SUPPLIER_MANAGER인 경우 소속 업체
) {
}
