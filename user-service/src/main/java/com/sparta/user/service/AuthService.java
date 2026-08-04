package com.sparta.user.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
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
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId(), user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .role(user.getRole())
                .build();
    }
}
