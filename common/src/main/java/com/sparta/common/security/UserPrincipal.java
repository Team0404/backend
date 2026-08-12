package com.sparta.common.security;

import com.sparta.common.entity.UserRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * 게이트웨이가 주입한 인증 헤더(X-User-*)로 구성되는 현재 로그인 사용자.
 * 컨트롤러에서 {@code @CurrentUser UserPrincipal principal} 로 주입받는다.
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal {

    private final UUID userId;
    private final String username;
    private final UserRole role;

    /** 주어진 역할 중 하나라도 일치하면 true */
    public boolean hasAnyRole(UserRole... roles) {
        for (UserRole r : roles) {
            if (this.role == r) {
                return true;
            }
        }
        return false;
    }
}
