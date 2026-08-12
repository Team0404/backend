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
     * 순서가 중요하다: "락 획득 → 멱등성 체크" 순으로 처리한다 (체크를 먼저 하지 않는 이유는
     * 아래 참고). referenceId가 없으면 예전처럼 매번 그대로 반영한다(Order가 아직 안 넘겨주는
     * 경우를 위한 하위 호환).
     *
     * ⚠️ "체크 먼저, 락 나중"으로 하면 두 요청이 락을 잡기 전에 동시에 exists 체크를 통과해버리는
     * race가 생긴다. 그러면 결국 같은 referenceId로 두 번 INSERT가 나가면서 DB 유니크 제약 위반이
     * 발생하는데, Postgres는 제약 위반이 한 번 나면 그 트랜잭션 전체를 "aborted" 상태로 만들어버려서
     * try-catch로 자바 예외만 잡아봐야 트랜잭션 자체는 이미 망가진 상태라 안전하지 않다.
     * 그래서 "락을 먼저 잡고" 그 다음에 체크하는 순서로 바꿔서, 같은 상품에 대한 동시 요청을
     * 애초에 직렬화시켜 이 race 자체가 발생하지 않게 만든다.
     */
    @Transactional
    public void decreaseStock(UUID productId, Integer quantity, String referenceId) {
        validateQuantity(quantity);

        Product product = getProductForUpdateOrThrow(productId); // 락 먼저 - 동시 요청을 여기서 직렬화

        if (referenceId != null && stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, referenceId, ProductStockMovement.MovementType.DECREASE)) {
            log.info("이미 처리된 재고 차감 요청 - 재적용하지 않음 (productId={}, referenceId={})", productId, referenceId);
            return;
        }

        product.decreaseStock(quantity);

        if (referenceId != null) {
            stockMovementRepository.save(ProductStockMovement.builder()
                    .productId(productId)
                    .referenceId(referenceId)
                    .movementType(ProductStockMovement.MovementType.DECREASE)
                    .quantity(quantity)
                    .build());
        }
    }

    /**
     * Order 서비스가 주문 취소 시 호출 (POST /{id}/restore-stock). decreaseStock과 동일한 "락 먼저,
     * 체크 나중" 순서로 처리한다 (이유는 decreaseStock 주석 참고).
     */
    @Transactional
    public void restoreStock(UUID productId, Integer quantity, String referenceId) {
        validateQuantity(quantity);

        Product product = getProductForUpdateOrThrow(productId);

        if (referenceId != null && stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, referenceId, ProductStockMovement.MovementType.RESTORE)) {
            log.info("이미 처리된 재고 복원 요청 - 재적용하지 않음 (productId={}, referenceId={})", productId, referenceId);
            return;
        }

        product.increaseStock(quantity);

        if (referenceId != null) {
            stockMovementRepository.save(ProductStockMovement.builder()
                    .productId(productId)
                    .referenceId(referenceId)
                    .movementType(ProductStockMovement.MovementType.RESTORE)
                    .quantity(quantity)
                    .build());
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