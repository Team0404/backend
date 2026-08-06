package com.sparta.user.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.user.dto.UserManagementResponse;
import com.sparta.user.entity.ApprovalStatus;
import com.sparta.user.entity.User;
import com.sparta.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void masterCanGetActiveUserList() {
        Pageable request = PageRequest.of(0, 25);
        User user = user(UserRole.SUPPLIER_MANAGER, null, UUID.randomUUID());
        when(userRepository.findAllByDeletedAtIsNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));

        PageResponse<UserManagementResponse> response = userService.getUsers(request);

        assertThat(response.content()).hasSize(1);
        assertThat(response.size()).isEqualTo(10);
        verify(userRepository).findAllByDeletedAtIsNull(PageRequest.of(0, 10));
    }

    @Test
    void masterCanApprovePendingUser() {
        UUID targetId = UUID.randomUUID();
        User target = user(UserRole.DELIVERY_MANAGER, null, null);
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(target));

        UserManagementResponse response = userService.approveUser(targetId, masterPrincipal());

        assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(target.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void hubManagerCanApproveUserInSameHub() {
        UUID managerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID hubId = UUID.randomUUID();
        User manager = user(UserRole.HUB_MANAGER, hubId, null);
        manager.approve();
        User target = user(UserRole.HUB_MANAGER, hubId, null);
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByUserIdAndDeletedAtIsNull(managerId)).thenReturn(Optional.of(manager));

        userService.approveUser(
                targetId,
                new UserPrincipal(managerId, "hub-manager", UserRole.HUB_MANAGER)
        );

        assertThat(target.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void hubManagerCannotApproveUserInDifferentHub() {
        UUID managerId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User manager = user(UserRole.HUB_MANAGER, UUID.randomUUID(), null);
        manager.approve();
        User target = user(UserRole.HUB_MANAGER, UUID.randomUUID(), null);
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(target));
        when(userRepository.findByUserIdAndDeletedAtIsNull(managerId)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> userService.approveUser(
                targetId,
                new UserPrincipal(managerId, "hub-manager", UserRole.HUB_MANAGER)
        )).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

        assertThat(target.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void approvedUserCannotBeReviewedAgain() {
        UUID targetId = UUID.randomUUID();
        User target = user(UserRole.DELIVERY_MANAGER, null, null);
        target.approve();
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.rejectUser(targetId, "재심사", masterPrincipal()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThat(target.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void masterCanRejectPendingUser() {
        UUID targetId = UUID.randomUUID();
        User target = user(UserRole.DELIVERY_MANAGER, null, null);
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(target));

        UserManagementResponse response = userService.rejectUser(targetId, "가입 정보 불일치", masterPrincipal());

        assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("가입 정보 불일치");
    }

    @Test
    void masterCanSoftDeleteUser() {
        UUID targetId = UUID.randomUUID();
        UUID masterId = UUID.randomUUID();
        User target = user(UserRole.SUPPLIER_MANAGER, null, UUID.randomUUID());
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.of(target));

        userService.deleteUser(targetId, masterId);

        assertThat(target.isDeleted()).isTrue();
        assertThat(target.getDeletedBy()).isEqualTo(masterId);
    }

    @Test
    void missingUserCannotBeReviewedOrDeleted() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findByUserIdAndDeletedAtIsNull(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.approveUser(targetId, masterPrincipal()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
        verify(userRepository, never()).save(any(User.class));
    }

    private UserPrincipal masterPrincipal() {
        return new UserPrincipal(UUID.randomUUID(), "master", UserRole.MASTER);
    }

    private User user(UserRole role, UUID hubId, UUID companyId) {
        return User.builder()
                .username(UUID.randomUUID().toString())
                .password("encoded-password")
                .nickname("nickname")
                .slackId(UUID.randomUUID().toString())
                .role(role)
                .hubId(hubId)
                .companyId(companyId)
                .build();
    }
}
