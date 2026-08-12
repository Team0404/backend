package com.sparta.company.company.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.common.entity.UserRole;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.company.dto.request.CompanyCreateRequest;
import com.sparta.company.company.dto.request.CompanyUpdateRequest;
import com.sparta.company.company.dto.response.CompanyResponse;
import com.sparta.company.company.entity.CompanyType;
import com.sparta.company.company.service.CompanyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CompanyController MockMvc 테스트.
 * 실제 Spring 컨텍스트를 띄우지 않고(standalone) 컨트롤러 하나만 검증한다.
 *
 * 컨트롤러는 이제 @AuthenticationPrincipal로 인증 사용자를 받고, 역할 검사는
 * @PreAuthorize(AOP)가 담당한다. standalone MockMvc는 서블릿 필터체인도, 메서드 시큐리티
 * AOP도 안 태우기 때문에 이 두 가지(GatewayAuthenticationFilter, @PreAuthorize)의 동작
 * 자체는 이 테스트의 범위가 아니다 - 여기서는 "인증된 사용자가 있을 때 컨트롤러가 서비스를
 * 올바르게 호출하고 응답을 올바르게 조립하는지"만 검증한다.
 *
 * 인증된 사용자를 흉내내기 위해, 요청 전에 SecurityContextHolder에 직접
 * Authentication을 넣어둔다 (AuthenticationPrincipalArgumentResolver가 여기서 꺼내감).
 */
@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private CompanyService companyService;

    private UUID masterId;
    private UUID hubId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        CompanyController controller = new CompanyController(companyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();

        masterId = UUID.randomUUID();
        hubId = UUID.randomUUID();
        companyId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        // 테스트 간 SecurityContext가 새어나가지 않도록 매번 정리
        SecurityContextHolder.clearContext();
    }

    /** MASTER로 로그인한 상태를 흉내낸다. */
    private void authenticateAsMaster() {
        authenticateAs(masterId, "master", UserRole.MASTER);
    }

    private void authenticateAs(UUID userId, String username, UserRole role) {
        UserPrincipal principal = new UserPrincipal(userId, username, role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("POST /api/v1/companies - 업체 생성 성공 시 201과 생성된 데이터를 반환한다")
    void create_success() throws Exception {
        // given
        authenticateAsMaster();

        CompanyCreateRequest request = new CompanyCreateRequest(
                "일산 건조식품 가공업체", CompanyType.PRODUCER, hubId, "경기도 고양시 일산동구 ...");

        CompanyResponse response = new CompanyResponse(
                companyId, request.name(), request.companyType(), hubId, request.address(),
                LocalDateTime.now(), null);

        given(companyService.create(any(CompanyCreateRequest.class), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/companies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("일산 건조식품 가공업체"))
                .andExpect(jsonPath("$.data.hubId").value(hubId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/companies/{id} - 업체명만 부분 수정한다")
    void update_success() throws Exception {
        // given
        authenticateAsMaster();

        CompanyUpdateRequest request = new CompanyUpdateRequest("수정된 이름", null, null, null);
        CompanyResponse response = new CompanyResponse(
                companyId, "수정된 이름", CompanyType.PRODUCER, hubId, "기존 주소",
                LocalDateTime.now(), LocalDateTime.now());

        given(companyService.update(eq(companyId), any(CompanyUpdateRequest.class), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/companies/{companyId}", companyId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된 이름"));
    }

    @Test
    @DisplayName("DELETE /api/v1/companies/{id} - 삭제 성공 시 200을 반환한다")
    void delete_success() throws Exception {
        // given
        authenticateAsMaster();

        // when & then
        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(companyService).delete(eq(companyId), any());
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} - 인증된 사용자는 단건 조회가 가능하다")
    void getOne_success() throws Exception {
        // given
        authenticateAsMaster();

        CompanyResponse response = new CompanyResponse(
                companyId, "부산 수산물 도매업체", CompanyType.RECEIVER, hubId, "부산 동구 ...",
                LocalDateTime.now(), null);
        given(companyService.getOne(companyId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/companies/{companyId}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("부산 수산물 도매업체"));
    }

    @Test
    @DisplayName("GET /api/v1/companies - 검색은 페이징 응답 포맷으로 반환한다")
    void search_success() throws Exception {
        // given
        authenticateAsMaster();

        CompanyResponse item = new CompanyResponse(
                companyId, "부산 수산물 도매업체", CompanyType.RECEIVER, hubId, "부산 동구 ...",
                LocalDateTime.now(), null);
        Page<CompanyResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        given(companyService.search(any(), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/companies")
                        .param("keyword", "수산물")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("부산 수산물 도매업체"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // 인증/인가(@PreAuthorize, GatewayAuthenticationFilter) 자체의 동작 검증은
    // 이 standalone 단위테스트의 범위 밖이라 여기서 다루지 않는다.
    // 필요해지면 @SpringBootTest + spring-security-test 기반 통합 테스트로 별도 작성한다.
}