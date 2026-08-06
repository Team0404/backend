package com.sparta.user.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.entity.UserRole;
import com.sparta.user.dto.LoginRequest;
import com.sparta.user.dto.LoginResponse;
import com.sparta.user.dto.SignupRequest;
import com.sparta.user.dto.SignupResponse;
import com.sparta.user.entity.User;
import com.sparta.user.jwt.JwtTokenProvider;
import com.sparta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateSignupPolicy(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 사용 중인 아이디입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .slackId(request.getSlackId())
                .role(request.getRole())
                .hubId(request.getHubId())
                .companyId(request.getCompanyId())
                .build();

        User saved = userRepository.save(user);

        return SignupResponse.builder()
                .userId(saved.getUserId())
                .approvalStatus(saved.getApprovalStatus())
                .build();
    }

    /**
     * 로그인. 아이디/비밀번호 검증 후 Access Token 을 발급
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(request.getUsername())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isApproved()) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED, "승인된 사용자만 로그인할 수 있습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .role(user.getRole())
                .build();
    }

    private void validateSignupPolicy(SignupRequest request) {
        UserRole role = request.getRole();
        if (role == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "역할은 필수입니다.");
        }

        switch (role) {
            case MASTER -> throw new BusinessException(
                    ErrorCode.INVALID_INPUT, "MASTER 역할은 공개 회원가입으로 신청할 수 없습니다.");
            case HUB_MANAGER -> {
                if (request.getHubId() == null || request.getCompanyId() != null) {
                    throw new BusinessException(
                            ErrorCode.INVALID_INPUT, "HUB_MANAGER는 hubId만 입력해야 합니다.");
                }
            }
            case SUPPLIER_MANAGER -> {
                if (request.getCompanyId() == null || request.getHubId() != null) {
                    throw new BusinessException(
                            ErrorCode.INVALID_INPUT, "SUPPLIER_MANAGER는 companyId만 입력해야 합니다.");
                }
            }
            case DELIVERY_MANAGER -> {
                if (request.getHubId() != null || request.getCompanyId() != null) {
                    throw new BusinessException(
                            ErrorCode.INVALID_INPUT,
                            "DELIVERY_MANAGER의 소속 정보는 배송 담당자 등록 과정에서 지정해야 합니다.");
                }
            }
        }
    }
}
