package com.sparta.user.controller;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.user.dto.LoginRequest;
import com.sparta.user.dto.LoginResponse;
import com.sparta.user.dto.SignupRequest;
import com.sparta.user.dto.SignupResponse;
import com.sparta.user.service.AuthService;
import com.sparta.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 및 로그인 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
        summary = "회원가입 요청",
        description = """
                    회원가입을 요청합니다.
                    
                    - username : 4~10자 영문 소문자 + 숫자
                    - password : 8~15자 영문 + 숫자 + 특수문자
                    - 가입 시 상태는 PENDING으로 저장
                    """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 username")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입 요청이 완료되었습니다.", response));
    }

    @Operation(
        summary = "로그인",
        description = """
                    승인(APPROVED) 상태의 사용자만 로그인할 수 있습니다.
                    
                    로그인 성공 시 JWT Access Token을 발급합니다.
                    """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "승인되지 않은 사용자")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    @Operation(summary = "내 정보 조회", description = "로그인된 사용자 본인의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@CurrentUser UserPrincipal principal) {
        return ApiResponse.success(userService.getUserInfo(principal.getUserId()));
    }
}
