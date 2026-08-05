package com.sparta.common.security;

import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayAuthenticationFilterTest {

    private final GatewayAuthenticationFilter filter = new GatewayAuthenticationFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAuthenticationFromGatewayHeaders() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, userId.toString());
        request.addHeader(AuthHeaders.USERNAME, "master-user");
        request.addHeader(AuthHeaders.USER_ROLE, UserRole.MASTER.name());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> captured = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> captured.set(SecurityContextHolder.getContext().getAuthentication()));

        Authentication authentication = captured.get();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(userId);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MASTER");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void leavesRequestAnonymousWhenAuthenticationHeadersAreAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> captured = new AtomicReference<>();

        filter.doFilter(request, response,
                (req, res) -> captured.set(SecurityContextHolder.getContext().getAuthentication()));

        assertThat(captured.get()).isNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsMalformedAuthenticationHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, "not-a-uuid");
        request.addHeader(AuthHeaders.USER_ROLE, "UNKNOWN_ROLE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("Invalid headers must not continue the filter chain");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotConvertDownstreamExceptionsToAuthenticationFailures() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthHeaders.USER_ID, UUID.randomUUID().toString());
        request.addHeader(AuthHeaders.USER_ROLE, UserRole.MASTER.name());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (req, res) -> {
            throw new IllegalArgumentException("business failure");
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("business failure");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
