package com.sparta.company.product.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.client.user.UserClient;
import com.sparta.company.client.user.UserQueryService;
import com.sparta.company.client.user.UserResponse;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.repository.CompanyRepository;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductSearchCondition;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.entity.Product;
import com.sparta.company.product.exception.ProductErrorCode;
import com.sparta.company.product.repository.ProductQueryRepository;
import com.sparta.company.product.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final CompanyRepository companyRepository;
    private final UserQueryService userQueryService;

    @Transactional
    public ProductResponse create(ProductCreateRequest request, UserPrincipal userPrincipal) {

        Company company = companyRepository.findByIdAndDeletedAtIsNull(request.companyId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.INVALID_COMPANY_ID));

        validateCreatePermission(company, userPrincipal);

        Product product = Product.builder()
                .name(request.name())
                .company(company)
                .hubId(company.getHubId())
                .price(request.price())
                .stockQuantity(request.initialStock())
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID productId, ProductUpdateRequest request, UserPrincipal userPrincipal) {

        Product product = getActiveProductOrThrow(productId);

        validateUpdatePermission(product, userPrincipal);

        product.update(request.name(), request.price());

        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(UUID productId, UserPrincipal userPrincipal) {

        Product product = getActiveProductOrThrow(productId);

        validateDeletePermission(product, userPrincipal);

        product.softDelete(userPrincipal.getUserId());
    }

    public ProductResponse getOne(UUID productId) {
        return ProductResponse.from(getActiveProductOrThrow(productId));
    }

    public Page<ProductResponse> search(ProductSearchCondition condition, Pageable pageable, UserPrincipal userPrincipal) {

        ProductSearchCondition scopedCondition = condition;

        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            scopedCondition = new ProductSearchCondition(condition.keyword(), condition.companyId(), myHubId);
        }

        return productQueryRepository.search(scopedCondition, pageable)
                .map(ProductResponse::from);
    }

    @Transactional
    public void decreaseStock(UUID productId, Integer quantity) {
        validateQuantity(quantity);
        Product product = getActiveProductOrThrow(productId);
        product.decreaseStock(quantity);
    }

    @Transactional
    public void restoreStock(UUID productId, Integer quantity) {
        validateQuantity(quantity);
        Product product = getActiveProductOrThrow(productId);
        product.increaseStock(quantity);
    }

    // 내부 검증 로직

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Product getActiveProductOrThrow(UUID productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateCreatePermission(Company company, UserPrincipal userPrincipal) {
        if (userPrincipal.hasAnyRole(UserRole.MASTER)) {
            return;
        }
        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            if (myHubId.equals(company.getHubId())) {
                return;
            }
            throw new BusinessException(ProductErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        if (userPrincipal.hasAnyRole(UserRole.SUPPLIER_MANAGER)) {
            UUID myCompanyId = getScopeCompanyId(userPrincipal);
            if (myCompanyId.equals(company.getId())) {
                return;
            }
            throw new BusinessException(ProductErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        throw new BusinessException(ProductErrorCode.FORBIDDEN_COMPANY_SCOPE);
    }

    private void validateUpdatePermission(Product product, UserPrincipal userPrincipal) {
        if (userPrincipal.hasAnyRole(UserRole.MASTER)) {
            return;
        }
        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            if (myHubId.equals(product.getHubId())) {
                return;
            }
            throw new BusinessException(ProductErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        if (userPrincipal.hasAnyRole(UserRole.SUPPLIER_MANAGER)) {
            UUID myCompanyId = getScopeCompanyId(userPrincipal);
            if (myCompanyId.equals(product.getCompanyId())) {
                return;
            }
            throw new BusinessException(ProductErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        throw new BusinessException(ProductErrorCode.FORBIDDEN_COMPANY_SCOPE);
    }

    private void validateDeletePermission(Product product, UserPrincipal userPrincipal) {
        if (userPrincipal.hasAnyRole(UserRole.MASTER)) {
            return;
        }
        if (userPrincipal.hasAnyRole(UserRole.HUB_MANAGER)) {
            UUID myHubId = getScopeHubId(userPrincipal);
            if (myHubId.equals(product.getHubId())) {
                return;
            }
        }
        throw new BusinessException(ProductErrorCode.FORBIDDEN_HUB_SCOPE);
    }

    private UUID getScopeHubId(UserPrincipal userPrincipal) {
        UserInfoResponse user = userQueryService.getUserInfo(userPrincipal.getUserId());
        if (user.getHubId() == null) {
            throw new BusinessException(ProductErrorCode.FORBIDDEN_HUB_SCOPE);
        }
        return user.getHubId();
    }

    private UUID getScopeCompanyId(UserPrincipal userPrincipal) {
        UserInfoResponse user = userQueryService.getUserInfo(userPrincipal.getUserId());
        if (user.getCompanyId() == null) {
            throw new BusinessException(ProductErrorCode.FORBIDDEN_COMPANY_SCOPE);
        }
        return user.getCompanyId();
    }
}
