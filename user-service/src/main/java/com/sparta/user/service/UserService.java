package com.sparta.user.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.common.util.PageableUtil;
import com.sparta.user.dto.UserManagementResponse;
import com.sparta.user.entity.ApprovalStatus;
import com.sparta.user.entity.User;
import com.sparta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 서비스 간 통신용 사용자 정보 조회. (삭제된 사용자는 조회되지 않음)
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .slackId(user.getSlackId())
                .role(user.getRole())
                .hubId(user.getHubId())
                .companyId(user.getCompanyId())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserManagementResponse> getUsers(Pageable pageable) {
        Page<UserManagementResponse> users = userRepository
                .findAllByDeletedAtIsNull(PageableUtil.normalize(pageable))
                .map(UserManagementResponse::from);
        return PageResponse.from(users);
    }

    @Transactional
    public UserManagementResponse approveUser(UUID userId, UserPrincipal principal) {
        User target = getActiveUser(userId);
        validateReviewPolicy(target, principal);
        validatePending(target);
        target.approve();
        return UserManagementResponse.from(target);
    }

    @Transactional
    public UserManagementResponse rejectUser(UUID userId, String reason, UserPrincipal principal) {
        User target = getActiveUser(userId);
        validateReviewPolicy(target, principal);
        validatePending(target);
        target.reject(reason);
        return UserManagementResponse.from(target);
    }

    @Transactional
    public void deleteUser(UUID userId, UUID deletedBy) {
        User target = getActiveUser(userId);
        target.softDelete(deletedBy);
    }

    private User getActiveUser(UUID userId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private void validateReviewPolicy(User target, UserPrincipal principal) {
        if (principal.getRole() == UserRole.MASTER) {
            return;
        }

        User manager = getActiveUser(principal.getUserId());
        if (manager.getRole() != UserRole.HUB_MANAGER
                || manager.getHubId() == null
                || !manager.getHubId().equals(target.getHubId())) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED, "담당 허브에 속한 사용자만 처리할 수 있습니다.");
        }
    }

    private void validatePending(User user) {
        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "대기 중인 가입 요청만 처리할 수 있습니다.");
        }
    }
}
