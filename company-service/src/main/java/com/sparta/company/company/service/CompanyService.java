package com.sparta.company.company.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.client.hub.HubClient;
import com.sparta.company.client.user.UserClient;
import com.sparta.company.client.user.UserResponse;
import com.sparta.company.company.dto.request.CompanyCreateRequest;
import com.sparta.company.company.dto.request.CompanySearchCondition;
import com.sparta.company.company.dto.request.CompanyUpdateRequest;
import com.sparta.company.company.dto.response.CompanyResponse;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.exception.CompanyErrorCode;
import com.sparta.company.company.repository.CompanyQueryRepository;
import com.sparta.company.company.repository.CompanyRepository;
import com.sparta.company.product.repository.ProductRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyQueryRepository companyQueryRepository;
    private final ProductRepository productRepository;
    private final HubClient hubClient;
    private final UserClient userClient;

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request, UserPrincipal userPrincipal) {

        validateHubExists(request.hubId());

        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            if (!myHubId.equals(request.hubId())) {
                throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
            }
        } else if (!userPrincipal.hasAnyRole(UserRole.MASTER)) {
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }

        Company company = Company.builder()
                .name(request.name())
                .companyType(request.companyType())
                .hubId(request.hubId())
                .address(request.address())
                .build();

        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(UUID companyId, CompanyUpdateRequest request, UserPrincipal userPrincipal) {

        Company company = getActiveCompanyOrThrow(companyId);

        validateUpdatePermission(company, userPrincipal);

        if (request.hubId() != null && !request.hubId().equals(company.getHubId())) {
            validateHubExists(request.hubId());
        }

        company.update(request.name(), request.companyType(), request.hubId(), request.address());

        return CompanyResponse.from(company);
    }

    @Transactional
    public void delete(UUID companyId, UserPrincipal userPrincipal) {

        Company company = getActiveCompanyOrThrow(companyId);

        validateDeletePermission(company, userPrincipal);

        company.softDelete(userPrincipal.getUserId());

        productRepository.findAllByCompany_IdAndDeletedAtIsNull(companyId)
                .forEach(product -> product.softDelete(userPrincipal.getUserId()));
    }

    public CompanyResponse getOne(UUID companyId) {
        return CompanyResponse.from(getActiveCompanyOrThrow(companyId));
    }

    public Page<CompanyResponse> search(CompanySearchCondition condition, Pageable pageable) {
        return companyQueryRepository.search(condition, pageable)
                .map(CompanyResponse::from);
    }

    // 내부 검증 로직
    private Company getActiveCompanyOrThrow(UUID companyId) {
        return companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.COMPANY_NOT_FOUND));
    }

    private void validateHubExists(UUID hubId) {
        try {
            hubClient.getHub(hubId.toString());
        } catch (FeignException.NotFound e) {
            throw new BusinessException(CompanyErrorCode.INVALID_HUB_ID);
        }
    }

    private void validateUpdatePermission(Company company, UserPrincipal userPrincipal) {
        if (userPrincipal.hasAnyRole(UserRole.MASTER)) {
            return;
        }
        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            if (myHubId.equals(company.getHubId())) {
                return;
            }
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        if (userPrincipal.hasAnyRole(UserRole.SUPPLIER_MANAGER)) {
            UUID myCompanyId = getScopeCompanyId(userPrincipal);
            if (myCompanyId.equals(company.getId())) {
                return;
            }
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
    }

    private void validateDeletePermission(Company company, UserPrincipal userPrincipal) {
        if (userPrincipal.hasAnyRole(UserRole.MASTER)) {
            return;
        }
        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            if (myHubId.equals(company.getHubId())) {
                return;
            }
        }
        throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
    }

    private UUID getScopeHubId(UserPrincipal userPrincipal) {
        UserResponse user = userClient.getUser(userPrincipal.getUserId().toString());
        if (user.hubId() == null) {
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        return user.hubId();
    }

    private UUID getScopeCompanyId(UserPrincipal userPrincipal) {
        UserResponse user = userClient.getUser(userPrincipal.getUserId().toString());
        if (user.companyId() == null) {
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        return user.companyId();
    }
}