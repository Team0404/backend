package com.sparta.company.product.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.common.util.PageableUtil;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductSearchCondition;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 생성: MASTER/HUB_MANAGER(담당 허브)/SUPPLIER_MANAGER(본인 업체) - 소유권은 서비스에서 검증
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('SUPPLIER_MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(productService.create(request, userPrincipal));
    }

    // 수정: 생성과 동일한 역할 (PATCH - name/price만 부분 수정)
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('SUPPLIER_MANAGER')")
    @PatchMapping("/{productId}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID productId,
                                               @RequestBody ProductUpdateRequest request,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(productService.update(productId, request, userPrincipal));
    }

    // Order 서비스가 주문 생성 시 호출 (재고 차감). 사람 인증 대상이 아니라 @PreAuthorize 없음.
    // referenceId(주로 주문 ID)는 선택값 - 넘기면 재시도로 중복 호출돼도 재고가 한 번만 반영됨(멱등성).
    // 아직 Order가 안 넘겨줄 수 있어 하위 호환을 위해 필수로 강제하지 않음.
    @PostMapping("/{id}/decrease-stock")
    public ApiResponse<Void> decreaseStock(@PathVariable("id") UUID productId,
                                           @RequestParam("quantity") Integer quantity,
                                           @RequestParam(value = "referenceId", required = false) String referenceId) {
        productService.decreaseStock(productId, quantity, referenceId);
        return ApiResponse.success(null);
    }

    // Order 서비스가 주문 취소 시 호출 (재고 복원). decrease-stock과 동일한 멱등성 처리.
    @PostMapping("/{id}/restore-stock")
    public ApiResponse<Void> restoreStock(@PathVariable("id") UUID productId,
                                          @RequestParam("quantity") Integer quantity,
                                          @RequestParam(value = "referenceId", required = false) String referenceId) {
        productService.restoreStock(productId, quantity, referenceId);
        return ApiResponse.success(null);
    }

    // 삭제: MASTER/HUB_MANAGER(담당 허브)만
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@PathVariable UUID productId,
                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        productService.delete(productId, userPrincipal);
        return ApiResponse.success(null);
    }

    // 조회/검색: 로그인만 되어 있으면 전체 허용
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getOne(@PathVariable UUID productId) {
        return ApiResponse.success(productService.getOne(productId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID hubId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ProductSearchCondition condition = new ProductSearchCondition(keyword, companyId, hubId);
        Pageable normalized = PageableUtil.normalize(pageable);

        Page<ProductResponse> result = productService.search(condition, normalized, userPrincipal);
        return ApiResponse.success(PageResponse.from(result));
    }
}
