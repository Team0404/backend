package com.sparta.user.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.user.dto.LoginRequest;
import com.sparta.user.dto.LoginResponse;
import com.sparta.user.dto.RefreshTokenRequest;
import com.sparta.user.dto.SignupRequest;
import com.sparta.user.dto.SignupResponse;
import com.sparta.user.dto.TokenRefreshResponse;
import com.sparta.user.entity.ApprovalStatus;
import com.sparta.user.entity.User;
import com.sparta.user.jwt.JwtTokenProvider;
import com.sparta.user.repository.RefreshTokenStore;
import com.sparta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private RefreshTokenStore refreshTokenStore;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, refreshTokenStore);
    }

    @Test
    void rejectsMasterSignup() {
        SignupRequest request = signupRequest(UserRole.MASTER, null, null);

        assertInvalidSignup(request);
    }

    @Test
    void rejectsInvalidHubManagerAffiliation() {
        assertInvalidSignup(signupRequest(UserRole.HUB_MANAGER, null, null));
        assertInvalidSignup(signupRequest(UserRole.HUB_MANAGER, UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void rejectsInvalidSupplierManagerAffiliation() {
        assertInvalidSignup(signupRequest(UserRole.SUPPLIER_MANAGER, null, null));
        assertInvalidSignup(signupRequest(UserRole.SUPPLIER_MANAGER, UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void rejectsDeliveryManagerAffiliation() {
        assertInvalidSignup(signupRequest(UserRole.DELIVERY_MANAGER, UUID.randomUUID(), null));
        assertInvalidSignup(signupRequest(UserRole.DELIVERY_MANAGER, null, UUID.randomUUID()));
    }

    @Test
    void createsPendingUserWithValidSignupPolicy() {
        SignupRequest request = signupRequest(UserRole.HUB_MANAGER, UUID.randomUUID(), null);
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SignupResponse response = authService.signup(request);

        assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void approvedUserCanLogin() {
        LoginRequest request = loginRequest();
        User user = user(ApprovalStatus.APPROVED);
        when(userRepository.findByUsernameAndDeletedAtIsNull("login-user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(Duration.ofDays(14));

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenStore).save(user.getUserId(), "refresh-token", Duration.ofDays(14));
    }

    @Test
    void validRefreshTokenIsRotated() {
        RefreshTokenRequest request = refreshTokenRequest("current-refresh-token");
        User user = user(ApprovalStatus.APPROVED);
        when(jwtTokenProvider.getUserIdFromRefreshToken("current-refresh-token")).thenReturn(user.getUserId());
        when(userRepository.findByUserIdAndDeletedAtIsNull(user.getUserId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(user.getUserId())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(Duration.ofDays(14));
        when(refreshTokenStore.rotate(
                user.getUserId(), "current-refresh-token", "new-refresh-token", Duration.ofDays(14)))
                .thenReturn(true);

        TokenRefreshResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void reusedRefreshTokenIsRejected() {
        RefreshTokenRequest request = refreshTokenRequest("old-refresh-token");
        User user = user(ApprovalStatus.APPROVED);
        when(jwtTokenProvider.getUserIdFromRefreshToken("old-refresh-token")).thenReturn(user.getUserId());
        when(userRepository.findByUserIdAndDeletedAtIsNull(user.getUserId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(user.getUserId())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(Duration.ofDays(14));
        when(refreshTokenStore.rotate(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void deletedUserCannotRefreshTokens() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = refreshTokenRequest("refresh-token");
        when(jwtTokenProvider.getUserIdFromRefreshToken("refresh-token")).thenReturn(userId);
        when(userRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verify(refreshTokenStore, never()).rotate(any(), any(), any(), any());
    }

    @Test
    void unapprovedUserCannotRefreshTokens() {
        RefreshTokenRequest request = refreshTokenRequest("refresh-token");
        User user = user(ApprovalStatus.PENDING);
        when(jwtTokenProvider.getUserIdFromRefreshToken("refresh-token")).thenReturn(user.getUserId());
        when(userRepository.findByUserIdAndDeletedAtIsNull(user.getUserId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        verify(refreshTokenStore, never()).rotate(any(), any(), any(), any());
    }

    @Test
    void validRefreshTokenCanLogout() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = refreshTokenRequest("refresh-token");
        when(jwtTokenProvider.getUserIdFromRefreshToken("refresh-token")).thenReturn(userId);
        when(refreshTokenStore.revoke(userId, "refresh-token")).thenReturn(true);

        authService.logout(request);

        verify(refreshTokenStore).revoke(userId, "refresh-token");
    }

    @Test
    void mismatchedRefreshTokenCannotLogout() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = refreshTokenRequest("refresh-token");
        when(jwtTokenProvider.getUserIdFromRefreshToken("refresh-token")).thenReturn(userId);
        when(refreshTokenStore.revoke(userId, "refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.logout(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void pendingUserCannotLogin() {
        assertApprovalStatusCannotLogin(ApprovalStatus.PENDING);
    }

    @Test
    void rejectedUserCannotLogin() {
        assertApprovalStatusCannotLogin(ApprovalStatus.REJECTED);
    }

    @Test
    void deletedUserCannotLogin() {
        LoginRequest request = loginRequest();
        when(userRepository.findByUsernameAndDeletedAtIsNull("login-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }

    @Test
    void wrongPasswordCannotLogin() {
        LoginRequest request = loginRequest();
        User user = user(ApprovalStatus.APPROVED);
        when(userRepository.findByUsernameAndDeletedAtIsNull("login-user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }

    private void assertApprovalStatusCannotLogin(ApprovalStatus status) {
        LoginRequest request = loginRequest();
        User user = user(status);
        when(userRepository.findByUsernameAndDeletedAtIsNull("login-user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));
        verify(jwtTokenProvider, never()).createAccessToken(any(), any(), any());
    }

    private void assertInvalidSignup(SignupRequest request) {
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        verify(userRepository, never()).save(any(User.class));
    }

    private SignupRequest signupRequest(UserRole role, UUID hubId, UUID companyId) {
        SignupRequest request = mock(SignupRequest.class);
        when(request.getUsername()).thenReturn("new-user");
        when(request.getPassword()).thenReturn("password");
        when(request.getNickname()).thenReturn("nickname");
        when(request.getSlackId()).thenReturn("slack-id");
        when(request.getRole()).thenReturn(role);
        when(request.getHubId()).thenReturn(hubId);
        when(request.getCompanyId()).thenReturn(companyId);
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getUsername()).thenReturn("login-user");
        when(request.getPassword()).thenReturn("password");
        return request;
    }

    private RefreshTokenRequest refreshTokenRequest(String token) {
        RefreshTokenRequest request = mock(RefreshTokenRequest.class);
        when(request.getRefreshToken()).thenReturn(token);
        return request;
    }

    private User user(ApprovalStatus status) {
        User user = User.builder()
                .username("login-user")
                .password("encoded-password")
                .nickname("nickname")
                .slackId("slack-id")
                .role(UserRole.SUPPLIER_MANAGER)
                .companyId(UUID.randomUUID())
                .build();
        ReflectionTestUtils.setField(user, "userId", UUID.randomUUID());
        if (status == ApprovalStatus.APPROVED) {
            user.approve();
        } else if (status == ApprovalStatus.REJECTED) {
            user.reject("rejected");
        }
        return user;
    }
}
