package com.sparta.company.company.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.common.constant.AuthHeaders;
import com.sparta.common.exception.GlobalExceptionHandler;
import com.sparta.common.security.CurrentUserArgumentResolver;
import com.sparta.company.company.dto.request.CompanyCreateRequest;
import com.sparta.company.company.dto.request.CompanyUpdateRequest;
import com.sparta.company.company.dto.response.CompanyResponse;
import com.sparta.company.company.entity.CompanyType;
import com.sparta.company.company.service.CompanyService;
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
 * CurrentUserArgumentResolver는 common의 실제 구현을 그대로 등록해서
 * X-User-Id/Username/Role 헤더 처리 로직까지 함께 검증한다.
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
                        new CurrentUserArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        masterId = UUID.randomUUID();
        hubId = UUID.randomUUID();
        companyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/companies - 업체 생성 성공 시 201과 생성된 데이터를 반환한다")
    void create_success() throws Exception {
        // given
        CompanyCreateRequest request = new CompanyCreateRequest(
                "일산 건조식품 가공업체", CompanyType.PRODUCER, hubId, "경기도 고양시 일산동구 ...");

        CompanyResponse response = new CompanyResponse(
                companyId, request.name(), request.companyType(), hubId, request.address(),
                LocalDateTime.now(), null);

        given(companyService.create(any(CompanyCreateRequest.class), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/companies")
                        .header(AuthHeaders.USER_ID, masterId.toString())
                        .header(AuthHeaders.USERNAME, "master")
                        .header(AuthHeaders.USER_ROLE, "MASTER")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("일산 건조식품 가공업체"))
                .andExpect(jsonPath("$.data.hubId").value(hubId.toString()));
    }

    @Test
    @DisplayName("인증 헤더가 없으면 생성 요청은 401로 거부된다")
    void create_fail_noAuthHeader() throws Exception {
        CompanyCreateRequest request = new CompanyCreateRequest(
                "업체", CompanyType.PRODUCER, hubId, "주소");

        mockMvc.perform(post("/api/v1/companies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                // GlobalExceptionHandler가 없는 standalone 환경이라 예외가 그대로 전파됨.
                // 실제 서버에서는 GlobalExceptionHandler가 401 응답으로 변환해준다.
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/v1/companies/{id} - 업체명만 부분 수정한다")
    void update_success() throws Exception {
        // given
        CompanyUpdateRequest request = new CompanyUpdateRequest("수정된 이름", null, null, null);
        CompanyResponse response = new CompanyResponse(
                companyId, "수정된 이름", CompanyType.PRODUCER, hubId, "기존 주소",
                LocalDateTime.now(), LocalDateTime.now());

        given(companyService.update(eq(companyId), any(CompanyUpdateRequest.class), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/companies/{companyId}", companyId)
                        .header(AuthHeaders.USER_ID, masterId.toString())
                        .header(AuthHeaders.USERNAME, "master")
                        .header(AuthHeaders.USER_ROLE, "MASTER")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된 이름"));
    }

    @Test
    @DisplayName("DELETE /api/v1/companies/{id} - 삭제 성공 시 200을 반환한다")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/v1/companies/{companyId}", companyId)
                        .header(AuthHeaders.USER_ID, masterId.toString())
                        .header(AuthHeaders.USERNAME, "master")
                        .header(AuthHeaders.USER_ROLE, "MASTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(companyService).delete(eq(companyId), any());
    }

    @Test
    @DisplayName("GET /api/v1/companies/{id} - 단건 조회는 인증 헤더 없이도 가능하다")
    void getOne_success() throws Exception {
        // given
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
}