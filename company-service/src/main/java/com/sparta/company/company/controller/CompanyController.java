package com.sparta.company.company.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.common.response.PageResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.common.util.PageableUtil;
import com.sparta.company.company.dto.request.CompanyCreateRequest;
import com.sparta.company.company.dto.request.CompanySearchCondition;
import com.sparta.company.company.dto.request.CompanyUpdateRequest;
import com.sparta.company.company.dto.response.CompanyResponse;
import com.sparta.company.company.entity.CompanyType;
import com.sparta.company.company.service.CompanyService;
import com.sparta.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    public ApiResponse<CompanyResponse> create(@Valid @RequestBody CompanyCreateRequest request,
//                                               @CurrentUser UserPrincipal userPrincipal,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(companyService.create(request, userPrincipal));
    }

    @PatchMapping("/{companyId}")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER') or hasRole('SUPPLIER_MANAGER')")
    public ApiResponse<CompanyResponse> update(@PathVariable UUID companyId,
                                               @RequestBody CompanyUpdateRequest request,
//                                               @CurrentUser UserPrincipal userPrincipal,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(companyService.update(companyId, request, userPrincipal));
    }

    @DeleteMapping("/{companyId}")
    @PreAuthorize("hasRole('MASTER') or hasRole('HUB_MANAGER')")
    public ApiResponse<Void> delete(@PathVariable UUID companyId,
//                                    @CurrentUser UserPrincipal userPrincipal,
                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        companyService.delete(companyId, userPrincipal);
        return ApiResponse.success(null);
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CompanyResponse> getOne(@PathVariable UUID companyId) {
        return ApiResponse.success(companyService.getOne(companyId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResponse<CompanyResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CompanyType companyType,
            @RequestParam(required = false) UUID hubId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        CompanySearchCondition condition = new CompanySearchCondition(keyword, companyType, hubId);
        Pageable normalized = PageableUtil.normalize(pageable);

        Page<CompanyResponse> result = companyService.search(condition, normalized);
        return ApiResponse.success(PageResponse.from(result));
    }
}
