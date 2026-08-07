package com.sparta.gateway.filter;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.gateway.security.AccessTokenBlacklistChecker;
import com.sparta.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private JwtTokenProvider jwtTokenProvider;
    private AccessTokenBlacklistChecker blacklistChecker;
    private GatewayFilterChain chain;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        blacklistChecker = mock(AccessTokenBlacklistChecker.class);
        chain = mock(GatewayFilterChain.class);
        filter = new JwtAuthFilter(jwtTokenProvider, blacklistChecker);
    }

    @Test
    void blacklistedAccessTokenIsRejected() {
        Claims claims = claims();
        MockServerWebExchange exchange = authenticatedExchange("/api/v1/auth/me");
        when(jwtTokenProvider.validateAndExtract("access-token")).thenReturn(claims);
        when(blacklistChecker.isBlacklisted("access-token-id")).thenReturn(Mono.just(true));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void validAccessTokenAddsTrustedAuthenticationHeaders() {
        Claims claims = claims();
        MockServerWebExchange exchange = authenticatedExchange("/api/v1/auth/me");
        when(jwtTokenProvider.validateAndExtract("access-token")).thenReturn(claims);
        when(blacklistChecker.isBlacklisted("access-token-id")).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst(AuthHeaders.USER_ID)).isEqualTo(claims.getSubject());
        assertThat(headers.getFirst(AuthHeaders.USERNAME)).isEqualTo("tester");
        assertThat(headers.getFirst(AuthHeaders.USER_ROLE)).isEqualTo("SUPPLIER_MANAGER");
        assertThat(headers.getFirst(AuthHeaders.TOKEN_ID)).isEqualTo("access-token-id");
        assertThat(headers.getFirst(AuthHeaders.TOKEN_EXPIRES_AT)).isEqualTo("123456789");
        assertThat(headers.containsKey(AuthHeaders.HUB_ID)).isFalse();
        assertThat(headers.containsKey(AuthHeaders.COMPANY_ID)).isFalse();
        assertThat(headers.containsKey(AuthHeaders.DELIVERY_MANAGER_ID)).isFalse();
    }

    @Test
    void redisFailureReturnsServiceUnavailable() {
        Claims claims = claims();
        MockServerWebExchange exchange = authenticatedExchange("/api/v1/auth/me");
        when(jwtTokenProvider.validateAndExtract("access-token")).thenReturn(claims);
        when(blacklistChecker.isBlacklisted("access-token-id"))
                .thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(chain, never()).filter(any());
    }

    @Test
    void publicRouteRemovesForgedAuthenticationHeaders() {
        MockServerWebExchange exchange = authenticatedExchange("/api/v1/auth/login");
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.containsKey(AuthHeaders.USER_ID)).isFalse();
        assertThat(headers.containsKey(AuthHeaders.TOKEN_ID)).isFalse();
        assertThat(headers.containsKey(AuthHeaders.HUB_ID)).isFalse();
        assertThat(headers.containsKey(AuthHeaders.COMPANY_ID)).isFalse();
        assertThat(headers.containsKey(AuthHeaders.DELIVERY_MANAGER_ID)).isFalse();
        verifyNoInteractions(jwtTokenProvider, blacklistChecker);
    }

    @Test
    void logoutRouteRequiresAccessToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/logout").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtTokenProvider, blacklistChecker, chain);
    }

    private MockServerWebExchange authenticatedExchange(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .header(AuthHeaders.USER_ID, UUID.randomUUID().toString())
                .header(AuthHeaders.TOKEN_ID, "forged-token-id")
                .header(AuthHeaders.HUB_ID, UUID.randomUUID().toString())
                .header(AuthHeaders.COMPANY_ID, UUID.randomUUID().toString())
                .header(AuthHeaders.DELIVERY_MANAGER_ID, UUID.randomUUID().toString())
                .build();
        return MockServerWebExchange.from(request);
    }

    private Claims claims() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(claims.getId()).thenReturn("access-token-id");
        when(claims.get("username", String.class)).thenReturn("tester");
        when(claims.get("role", String.class)).thenReturn("SUPPLIER_MANAGER");
        when(claims.getExpiration()).thenReturn(new Date(123456789L));
        return claims;
    }
}
