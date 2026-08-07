package com.sparta.company.product.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.common.util.PageableUtil;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductSearchCondition;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.service.ProductService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request,
                                                             @CurrentUser UserPrincipal userPrincipal) {
        return ApiResponse.success(productService.create(request, userPrincipal));
    }

    @PatchMapping("/{productId}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID productId,
                                               @RequestBody ProductUpdateRequest request,
                                               @CurrentUser UserPrincipal userPrincipal) {
        return ApiResponse.success(productService.update(productId, request, userPrincipal));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@PathVariable UUID productId,
                                    @CurrentUser UserPrincipal userPrincipal) {
        productService.delete(productId, userPrincipal);
        return ApiResponse.success(null);
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getOne(@PathVariable UUID productId) {
        return ApiResponse.success(productService.getOne(productId));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID hubId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentUser UserPrincipal userPrincipal
    ) {
        ProductSearchCondition condition = new ProductSearchCondition(keyword, companyId, hubId);
        Pageable normalized = PageableUtil.normalize(pageable);

        Page<ProductResponse> result = productService.search(condition, normalized, userPrincipal);
        return ApiResponse.success(PageResponse.from(result));
    }

    // Order에서 주문 생성 시 호출 (재고 차감)
    @PostMapping("/{id}/decrease-stock")
    public ApiResponse<Void> decreaseStock(@PathVariable("id") UUID productId,
                                           @RequestParam("quantity") Integer quantity){
        productService.decreaseStock(productId, quantity);
        return ApiResponse.success(null);
    }

    // Order에서 주문 취소 시 호출 (재고 복원)
    @PostMapping("/{id}/restore-stock")
    public ApiResponse<Void> restoreStock(@PathVariable("id") UUID productId,
                                          @RequestParam("quantity") Integer quantity){
        productService.restoreStock(productId, quantity);
        return ApiResponse.success(null);
    }

}
