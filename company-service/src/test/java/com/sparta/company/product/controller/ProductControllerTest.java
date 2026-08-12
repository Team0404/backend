package com.sparta.company.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.common.entity.UserRole;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.service.ProductService;
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
 * ProductController MockMvc 테스트.
 * CompanyControllerTest와 동일한 방식(standalone + SecurityContextHolder 직접 세팅)으로
 * @AuthenticationPrincipal을 흉내낸다. @PreAuthorize/필터체인 검증은 범위 밖.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ProductService productService;

    private UUID masterId;
    private UUID companyId;
    private UUID hubId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();

        masterId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        hubId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsMaster() {
        UserPrincipal principal = new UserPrincipal(masterId, "master", UserRole.MASTER);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MASTER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("POST /api/v1/products - 상품 생성 성공 시 201과 생성된 데이터를 반환한다")
    void create_success() throws Exception {
        // given
        authenticateAsMaster();

        ProductCreateRequest request = new ProductCreateRequest(
                "마른오징어 가공품", companyId, 15000L, 200L);

        ProductResponse response = new ProductResponse(
                productId, request.name(), companyId, hubId, 15000L, 200L,
                LocalDateTime.now(), null);

        given(productService.create(any(ProductCreateRequest.class), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("마른오징어 가공품"))
                .andExpect(jsonPath("$.data.stockQuantity").value(200));
    }

    @Test
    @DisplayName("PATCH /api/v1/products/{id} - 상품명만 부분 수정한다")
    void update_success() throws Exception {
        // given
        authenticateAsMaster();

        ProductUpdateRequest request = new ProductUpdateRequest("수정된 상품명", null);
        ProductResponse response = new ProductResponse(
                productId, "수정된 상품명", companyId, hubId, 15000L, 200L,
                LocalDateTime.now(), LocalDateTime.now());

        given(productService.update(eq(productId), any(ProductUpdateRequest.class), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/products/{productId}", productId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된 상품명"));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} - 삭제 성공 시 200을 반환한다")
    void delete_success() throws Exception {
        // given
        authenticateAsMaster();

        // when & then
        mockMvc.perform(delete("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).delete(eq(productId), any());
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - 인증된 사용자는 단건 조회가 가능하다")
    void getOne_success() throws Exception {
        // given
        authenticateAsMaster();

        ProductResponse response = new ProductResponse(
                productId, "마른오징어 가공품", companyId, hubId, 15000L, 150L,
                LocalDateTime.now(), null);
        given(productService.getOne(productId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("마른오징어 가공품"));
    }

    @Test
    @DisplayName("GET /api/v1/products - 검색은 페이징 응답 포맷으로 반환한다")
    void search_success() throws Exception {
        // given
        authenticateAsMaster();

        ProductResponse item = new ProductResponse(
                productId, "마른오징어 가공품", companyId, hubId, 15000L, 150L,
                LocalDateTime.now(), null);
        Page<ProductResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        given(productService.search(any(), any(), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "오징어"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("마른오징어 가공품"));
    }

    @Test
    @DisplayName("POST /api/v1/products/{id}/decrease-stock - Order 서비스의 재고 차감 요청을 처리한다 (인증 불필요)")
    void decreaseStock_success() throws Exception {
        // 이 엔드포인트는 @PreAuthorize/@AuthenticationPrincipal이 없어서 인증 상태와 무관하게 동작해야 함
        mockMvc.perform(post("/api/v1/products/{id}/decrease-stock", productId)
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).decreaseStock(productId, 50, null);
    }

    @Test
    @DisplayName("POST /api/v1/products/{id}/restore-stock - Order 서비스의 재고 복원 요청을 처리한다 (인증 불필요)")
    void restoreStock_success() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/restore-stock", productId)
                        .param("quantity", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productService).restoreStock(productId, 50, null);
    }

    @Test
    @DisplayName("POST /api/v1/products/{id}/decrease-stock - referenceId를 넘기면 그대로 전달된다")
    void decreaseStock_success_withReferenceId() throws Exception {
        mockMvc.perform(post("/api/v1/products/{id}/decrease-stock", productId)
                        .param("quantity", "50")
                        .param("referenceId", "order-123"))
                .andExpect(status().isOk());

        verify(productService).decreaseStock(productId, 50, "order-123");
    }
}