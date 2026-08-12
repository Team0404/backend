package com.sparta.gateway.filter;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.gateway.security.AccessTokenBlacklistChecker;
import com.sparta.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklistChecker blacklistChecker;

    // 인증 없이 통과할 공개 경로 (회원가입/로그인/토큰재발급 + actuator)
    // 그 외 /api/v1/auth/** (me, users, approve 등)는 인증 필요
    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        ServerWebExchange sanitizedExchange = removeAuthenticationHeaders(exchange);

        if (isWhitelisted(path)) {
            return chain.filter(sanitizedExchange);
        }

        String token = resolveToken(sanitizedExchange.getRequest());
        if (token == null) {
            return onUnauthorized(sanitizedExchange);
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.validateAndExtract(token);
        } catch (Exception e) {
            log.warn("JWT validation failed [{}]: {}", path, e.getMessage());
            return onUnauthorized(sanitizedExchange);
        }

        Mono<Boolean> blacklistLookup = blacklistChecker.isBlacklisted(claims.getId())
                .onErrorMap(BlacklistLookupException::new);

        return blacklistLookup
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        log.warn("Blacklisted access token rejected [{}]: {}", path, claims.getId());
                        return onUnauthorized(sanitizedExchange);
                    }
                    return chain.filter(withAuthenticationHeaders(sanitizedExchange, claims));
                })
                .onErrorResume(BlacklistLookupException.class, e -> {
                    log.error("Access token blacklist lookup failed [{}]", path, e.getCause());
                    return onServiceUnavailable(sanitizedExchange);
                });
    }

    private ServerWebExchange removeAuthenticationHeaders(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(AuthHeaders.USER_ID);
                    headers.remove(AuthHeaders.USERNAME);
                    headers.remove(AuthHeaders.USER_ROLE);
                    headers.remove(AuthHeaders.HUB_ID);
                    headers.remove(AuthHeaders.COMPANY_ID);
                    headers.remove(AuthHeaders.DELIVERY_MANAGER_ID);
                    headers.remove(AuthHeaders.TOKEN_ID);
                    headers.remove(AuthHeaders.TOKEN_EXPIRES_AT);
                }))
                .build();
    }

    private ServerWebExchange withAuthenticationHeaders(ServerWebExchange exchange, Claims claims) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.set(AuthHeaders.USER_ID, claims.getSubject());
                    headers.set(AuthHeaders.USERNAME, claims.get("username", String.class));
                    headers.set(AuthHeaders.USER_ROLE, claims.get("role", String.class));
                    setHeaderIfPresent(headers, AuthHeaders.HUB_ID,
                            claims.get("hubId", String.class));
                    setHeaderIfPresent(headers, AuthHeaders.COMPANY_ID,
                            claims.get("companyId", String.class));
                    headers.set(AuthHeaders.TOKEN_ID, claims.getId());
                    headers.set(AuthHeaders.TOKEN_EXPIRES_AT,
                            String.valueOf(claims.getExpiration().getTime()));
                }))
                .build();
    }

    private void setHeaderIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private boolean isWhitelisted(String path) {
        // Swagger 통합 UI / 문서 프록시(/{service}/v3/api-docs 포함) 는 인증 없이 허용
        if (path.startsWith("/swagger-ui") || path.contains("/v3/api-docs")) {
            return true;
        }
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> onServiceUnavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private static final class BlacklistLookupException extends RuntimeException {

        private BlacklistLookupException(Throwable cause) {
            super(cause);
        }
    }
}
