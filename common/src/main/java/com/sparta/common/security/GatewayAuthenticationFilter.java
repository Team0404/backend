package com.sparta.common.security;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Gateway가 검증한 X-User-* 헤더를 Spring Security 인증 정보로 변환한다.
 * 역할은 {@code ROLE_MASTER} 형식으로 등록되므로 {@code hasRole('MASTER')}와 호환된다.
 */
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader(AuthHeaders.USER_ID);
        String username = request.getHeader(AuthHeaders.USERNAME);
        String roleHeader = request.getHeader(AuthHeaders.USER_ROLE);

        if (isBlank(userIdHeader) && isBlank(roleHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isBlank(userIdHeader) || isBlank(roleHeader)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid authentication headers");
            return;
        }

        UserPrincipal principal;
        try {
            UserRole role = UserRole.valueOf(roleHeader);
            principal = new UserPrincipal(UUID.fromString(userIdHeader), username, role);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid authentication headers");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(ROLE_PREFIX + principal.getRole().name()))
                );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
