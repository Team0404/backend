package com.sparta.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.common.config.SecurityConfig;
import com.sparta.common.config.WebConfig;
import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.GlobalExceptionHandler;
import com.sparta.common.response.PageResponse;
import com.sparta.user.dto.UserManagementResponse;
import com.sparta.user.dto.TokenRefreshResponse;
import com.sparta.user.entity.ApprovalStatus;
import com.sparta.user.service.AuthService;
import com.sparta.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, WebConfig.class, GlobalExceptionHandler.class})
class AuthControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/auth/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @Test
    void masterCanGetUserList() throws Exception {
        given(userService.getUsers(any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, false));

        mockMvc.perform(get(BASE_URL)
                        .headers(authenticationHeaders(UserRole.MASTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void hubManagerCannotGetUserList() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .headers(authenticationHeaders(UserRole.HUB_MANAGER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUsers(any(Pageable.class));
    }

    @Test
    void hubManagerCanCallApproveApi() throws Exception {
        UUID targetId = UUID.randomUUID();
        UserManagementResponse response = UserManagementResponse.builder()
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();
        given(userService.approveUser(eq(targetId), any())).willReturn(response);

        mockMvc.perform(patch(BASE_URL + "/{userId}/approve", targetId)
                        .headers(authenticationHeaders(UserRole.HUB_MANAGER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus").value("APPROVED"));
    }

    @Test
    void supplierManagerCannotCallApproveApi() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(patch(BASE_URL + "/{userId}/approve", targetId)
                        .headers(authenticationHeaders(UserRole.SUPPLIER_MANAGER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).approveUser(eq(targetId), any());
    }

    @Test
    void rejectApiValidatesReason() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(patch(BASE_URL + "/{userId}/reject", targetId)
                        .headers(authenticationHeaders(UserRole.MASTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectRequestBody(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshApiReturnsRotatedTokenPair() throws Exception {
        given(authService.refresh(any())).willReturn(TokenRefreshResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequestBody("current-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
    }

    @Test
    void logoutApiRevokesRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequestBody("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(any());
    }

    @Test
    void refreshApiRequiresRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequestBody(""))))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.http.HttpHeaders authenticationHeaders(UserRole role) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(AuthHeaders.USER_ID, UUID.randomUUID().toString());
        headers.add(AuthHeaders.USERNAME, "tester");
        headers.add(AuthHeaders.USER_ROLE, role.name());
        return headers;
    }

    private record RejectRequestBody(String reason) {
    }

    private record RefreshTokenRequestBody(String refreshToken) {
    }
}
