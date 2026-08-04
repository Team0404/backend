package com.sparta.company.company.service;

import com.sparta.common.exception.BusinessException;
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
import com.sparta.company.security.AuthUser;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyQueryRepository companyQueryRepository;
    private final ProductRepository productRepository;
    private final HubClient hubClient;
    private final UserClient userClient;

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request, AuthUser authUser) {

        validateHubExists(request.hubId());

        if (authUser.isHubManager()) {
            UUID myHubId = getScopeHubId(authUser);
            if (!myHubId.equals(request.hubId())) {
                throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
            }
        } else if (!authUser.isMaster()) {
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
    public CompanyResponse update(UUID companyId, CompanyUpdateRequest request, AuthUser authUser) {

        Company company = getActiveCompanyOrThrow(companyId);

        validateUpdatePermission(company, authUser);

        if (request.hubId() != null && !request.hubId().equals(company.getHubId())) {
            validateHubExists(request.hubId());
        }

        company.update(request.name(), request.companyType(), request.hubId(), request.address());

        return CompanyResponse.from(company);
    }

    @Transactional
    public void delete(UUID companyId, AuthUser authUser) {

        Company company = getActiveCompanyOrThrow(companyId);

        validateDeletePermission(company, authUser);

        company.softDelete(authUser.userId());

        // 업체가 삭제되면 소속된 활성 상품도 함께 논리 삭제 처리
        productRepository.findAllByCompany_IdAndDeletedAtIsNull(companyId)
                .forEach(product -> product.softDelete(authUser.userId()));
    }

    public CompanyResponse getOne(UUID companyId) {
        return CompanyResponse.from(getActiveCompanyOrThrow(companyId));
    }

    public Page<CompanyResponse> search(CompanySearchCondition condition, Pageable pageable) {
        return companyQueryRepository.search(condition, pageable)
                .map(CompanyResponse::from);
    }


    // 내부 검증 로직 ------------------------------------------------------------------


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

    private void validateUpdatePermission(Company company, AuthUser authUser) {
        if (authUser.isMaster()) {
            return;
        }
        if (authUser.isHubManager()) {
            UUID myHubId = getScopeHubId(authUser);
            if (myHubId.equals(company.getHubId())) {
                return;
            }
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        if (authUser.isSupplierManager()) {
            UUID myCompanyId = getScopeCompanyId(authUser);
            if (myCompanyId.equals(company.getId())) {
                return;
            }
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
    }

    private void validateDeletePermission(Company company, AuthUser authUser) {
        if (authUser.isMaster()) {
            return;
        }
        if (authUser.isHubManager()) {
            UUID myHubId = getScopeHubId(authUser);
            if (myHubId.equals(company.getHubId())) {
                return;
            }
        }
        throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
    }

    private UUID getScopeHubId(AuthUser authUser) {
        UserResponse user = userClient.getUser(authUser.userId().toString());
        if (user.hubId() == null) {
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        return user.hubId();
    }

    private UUID getScopeCompanyId(AuthUser authUser) {
        UserResponse user = userClient.getUser(authUser.userId().toString());
        if (user.companyId() == null) {
            throw new BusinessException(CompanyErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        return user.companyId();
    }
}
