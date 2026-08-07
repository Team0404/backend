package com.sparta.common.constant;

/**
 * 게이트웨이가 JWT 검증 후 다운스트림 서비스로 전달하는 인증 정보 헤더 이름.
 * 각 서비스는 이 헤더로 인증된 사용자/권한을 신뢰한다.
 */
public final class AuthHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USERNAME = "X-Username";
    public static final String USER_ROLE = "X-User-Role";
    public static final String HUB_ID = "X-Hub-Id";
    public static final String COMPANY_ID = "X-Company-Id";
    public static final String DELIVERY_MANAGER_ID = "X-Delivery-Manager-Id";
    public static final String TOKEN_ID = "X-Token-Id";
    public static final String TOKEN_EXPIRES_AT = "X-Token-Expires-At";
    public static final String INTERNAL_CALL = "X-Internal-Call";

    private AuthHeaders() {
    }
}
