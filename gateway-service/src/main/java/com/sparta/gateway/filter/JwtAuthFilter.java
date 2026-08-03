package com.sparta.gateway.filter;

import com.sparta.common.constant.AuthHeaders;
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

    // 인증 없이 통과할 경로 (user-service의 로그인/회원가입 + 공통 엔드포인트)
    private static final List<String> WHITELIST = List.of(
            "/api/auth/",
            "/api/users/signup",
            "/actuator/",
            "/swagger-ui/",
            "/v3/api-docs/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());
        if (token == null) {
            return onUnauthorized(exchange);
        }

        try {
            Claims claims = jwtTokenProvider.validateAndExtract(token);
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header(AuthHeaders.USER_ID, claims.getSubject())
                            .header(AuthHeaders.USERNAME, claims.get("username", String.class))
                            .header(AuthHeaders.USER_ROLE, claims.get("role", String.class))
                    )
                    .build();
            return chain.filter(mutated);
        } catch (Exception e) {
            log.warn("JWT validation failed [{}]: {}", path, e.getMessage());
            return onUnauthorized(exchange);
        }
    }

    private boolean isWhitelisted(String path) {
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

    @Override
    public int getOrder() {
        return -1;
    }
}
