package com.sparta.company.product.service;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.client.user.UserQueryService;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.repository.CompanyRepository;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductSearchCondition;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.entity.Product;
import com.sparta.company.product.entity.ProductStockMovement;
import com.sparta.company.product.exception.ProductErrorCode;
import com.sparta.company.product.repository.ProductQueryRepository;
import com.sparta.company.product.repository.ProductRepository;
import com.sparta.company.product.repository.ProductStockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductStockMovementRepository stockMovementRepository;
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

    /**
     * Order 서비스가 주문 생성 시 호출 (POST /{id}/decrease-stock).
     *
     * - referenceId가 있으면: 같은 referenceId로 이미 처리된 차감 요청인지 먼저 확인해서,
     *   재시도로 중복 호출돼도 재고가 두 번 깎이지 않게 막는다(멱등성).
     * - referenceId가 없으면: 예전처럼 매번 그대로 반영한다 (Order가 아직 안 넘겨주는 경우 대비, 하위 호환).
     * - 재고 행 자체는 findByIdForUpdate로 잠가서, 동시에 여러 주문이 들어와도
     *   순차적으로 처리되게 한다 (Lost Update 방지).
     */
    @Transactional
    public void decreaseStock(UUID productId, Integer quantity, String referenceId) {
        validateQuantity(quantity);

        if (referenceId != null && stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, referenceId, ProductStockMovement.MovementType.DECREASE)) {
            log.info("이미 처리된 재고 차감 요청 - 재적용하지 않음 (productId={}, referenceId={})", productId, referenceId);
            return;
        }

        try {
            Product product = getProductForUpdateOrThrow(productId);
            product.decreaseStock(quantity);

            if (referenceId != null) {
                stockMovementRepository.save(ProductStockMovement.builder()
                        .productId(productId)
                        .referenceId(referenceId)
                        .movementType(ProductStockMovement.MovementType.DECREASE)
                        .quantity(quantity)
                        .build());
            }
        } catch (DataIntegrityViolationException e) {
            // exists 체크와 save 사이 짧은 순간에 동시에 같은 referenceId로 요청이 들어온 경우.
            // DB 유니크 제약이 막아준 것이므로, 실패로 보지 않고 "이미 처리됨"으로 조용히 종료한다.
            log.info("동시 중복 요청으로 재고 차감이 이미 다른 트랜잭션에서 처리됨 (productId={}, referenceId={})",
                    productId, referenceId);
        }
    }

    /**
     * Order 서비스가 주문 취소 시 호출 (POST /{id}/restore-stock). decreaseStock과 동일한 멱등성/락 처리.
     */
    @Transactional
    public void restoreStock(UUID productId, Integer quantity, String referenceId) {
        validateQuantity(quantity);

        if (referenceId != null && stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, referenceId, ProductStockMovement.MovementType.RESTORE)) {
            log.info("이미 처리된 재고 복원 요청 - 재적용하지 않음 (productId={}, referenceId={})", productId, referenceId);
            return;
        }

        try {
            Product product = getProductForUpdateOrThrow(productId);
            product.increaseStock(quantity);

            if (referenceId != null) {
                stockMovementRepository.save(ProductStockMovement.builder()
                        .productId(productId)
                        .referenceId(referenceId)
                        .movementType(ProductStockMovement.MovementType.RESTORE)
                        .quantity(quantity)
                        .build());
            }
        } catch (DataIntegrityViolationException e) {
            log.info("동시 중복 요청으로 재고 복원이 이미 다른 트랜잭션에서 처리됨 (productId={}, referenceId={})",
                    productId, referenceId);
        }
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

    // ----------------------------------------------------------------
    // 내부 검증 로직
    // ----------------------------------------------------------------

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Product getActiveProductOrThrow(UUID productId) {
        return productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    /** 재고 변경 전용 - 행 잠금 조회. 잠금 대기 타임아웃 시 명확한 에러로 변환한다. */
    private Product getProductForUpdateOrThrow(UUID productId) {
        try {
            return productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            log.warn("재고 행 잠금 획득 실패 (productId={})", productId, e);
            throw new BusinessException(ProductErrorCode.STOCK_LOCK_TIMEOUT);
        }
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
