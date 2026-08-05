package com.sparta.order.service;

import com.sparta.common.entity.UserRole;

import java.util.UUID;

/**
 * 로그인 사용자의 정보를 Service까지 전달하기 위한 객체
 * 권한 판단을 위해
 *
 * @param requestUserId
 * @param userRole
 * @param requestHubId
 * @param requestCompanyId
 * @param requestDeliveryManagerId
 */
public record OrderServiceContext(
        UUID requestUserId,
        UserRole userRole,
        UUID requestHubId,
        UUID requestCompanyId,
        UUID requestDeliveryManagerId
) {
}
