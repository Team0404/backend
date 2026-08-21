package com.sparta.company.product.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.client.user.UserQueryService;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.entity.CompanyType;
import com.sparta.company.company.repository.CompanyRepository;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.entity.Product;
import com.sparta.company.product.entity.ProductStockMovement;
import com.sparta.company.product.repository.ProductQueryRepository;
import com.sparta.company.product.repository.ProductRepository;
import com.sparta.company.product.repository.ProductStockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 상품(Product) CRUD + 재고(decrease/restore-stock) 서비스 계층 단위 테스트.
 * 락(findByIdForUpdate)은 Mockito 단위 테스트로는 실제 동시성 검증이 안 되므로
 * (실제 DB 행 잠금 동작 자체는 통합 테스트/수동 부하테스트가 필요),
 * 여기서는 "락 조회 메서드가 호출되는지"와 "referenceId로 중복 요청이 걸러지는지"만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductQueryRepository productQueryRepository;
    @Mock
    private ProductStockMovementRepository stockMovementRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private ProductService productService;

    private UUID hubId;
    private UUID companyId;
    private UUID productId;
    private Company company;
    private UserPrincipal masterUser;

    @BeforeEach
    void setUp() {
        hubId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        productId = UUID.randomUUID();
        company = Company.builder()
                .name("일산 건조식품 가공업체")
                .companyType(CompanyType.PRODUCER)
                .hubId(hubId)
                .address("경기도 고양시 ...")
                .build();
        masterUser = new UserPrincipal(UUID.randomUUID(), "master", UserRole.MASTER);
    }

    @Test
    @DisplayName("상품 생성 시 hubId는 업체의 hubId를 그대로 복사해서 저장한다")
    void create_success_copiesHubIdFromCompany() {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "마른오징어 가공품", companyId, 15000L, 200L);

        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));
        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductResponse response = productService.create(request, masterUser);

        // then
        assertThat(response.name()).isEqualTo("마른오징어 가공품");
        assertThat(response.hubId()).isEqualTo(hubId);
        assertThat(response.price()).isEqualTo(15000L);
        assertThat(response.stockQuantity()).isEqualTo(200L);
    }

    @Test
    @DisplayName("존재하지 않는 companyId로 생성하면 예외가 발생한다")
    void create_fail_invalidCompany() {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "테스트 상품", companyId, 1000L, 0L);
        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.create(request, masterUser))
                .isInstanceOf(BusinessException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("상품명을 부분 수정(PATCH)할 수 있다")
    void update_success() {
        // given
        Product product = Product.builder()
                .name("수정 전 상품명")
                .company(company)
                .hubId(hubId)
                .price(1000L)
                .stockQuantity(10L)
                .build();
        given(productRepository.findByIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(product));

        ProductUpdateRequest request = new ProductUpdateRequest("수정 후 상품명", null);

        // when
        ProductResponse response = productService.update(productId, request, masterUser);

        // then
        assertThat(response.name()).isEqualTo("수정 후 상품명");
        assertThat(response.price()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("MASTER가 상품을 삭제하면 논리 삭제된다")
    void delete_success() {
        // given
        Product product = Product.builder()
                .name("삭제될 상품")
                .company(company)
                .hubId(hubId)
                .price(1000L)
                .stockQuantity(10L)
                .build();
        given(productRepository.findByIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(product));

        // when
        productService.delete(productId, masterUser);

        // then
        assertThat(product.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("referenceId 없이 재고 차감 요청하면 예전처럼 매번 그대로 반영된다 (하위 호환)")
    void decreaseStock_success_withoutReferenceId() {
        // given
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(100L)
                .build();
        given(productRepository.findByIdForUpdate(productId))
                .willReturn(Optional.of(product));

        // when
        productService.decreaseStock(productId, 30, null);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(70L);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("referenceId가 있으면 재고 차감 후 이력을 남긴다")
    void decreaseStock_success_withReferenceId() {
        // given
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(100L)
                .build();
        String orderId = "order-123";

        given(stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, orderId, ProductStockMovement.MovementType.DECREASE))
                .willReturn(false);
        given(productRepository.findByIdForUpdate(productId))
                .willReturn(Optional.of(product));

        // when
        productService.decreaseStock(productId, 30, orderId);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(70L);
        verify(stockMovementRepository, times(1)).save(any(ProductStockMovement.class));
    }

    @Test
    @DisplayName("같은 referenceId로 재고 차감 요청이 재시도돼도 재고가 두 번 깎이지 않는다 (멱등성)")
    void decreaseStock_idempotent_sameReferenceIdSkipped() {
        // given
        String orderId = "order-123";
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(70L) // 이미 첫 요청 때 100 -> 70으로 반영된 상태를 가정
                .build();

        // 락(findByIdForUpdate)은 동시 요청을 직렬화하기 위해 항상 먼저 호출됨
        given(productRepository.findByIdForUpdate(productId)).willReturn(Optional.of(product));
        given(stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, orderId, ProductStockMovement.MovementType.DECREASE))
                .willReturn(true); // 이미 처리된 요청

        // when
        productService.decreaseStock(productId, 30, orderId);

        // then - 락은 잡았지만(직렬화 목적), 이미 처리된 요청이라 재고/이력은 그대로여야 함
        assertThat(product.getStockQuantity()).isEqualTo(70L); // 30 추가 차감 안 됨
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("동일한 DECREASE referenceId로 두 번 호출해도 재고는 한 번만 차감된다")
    void decreaseStock_sameReferenceIdTwice_decreasesOnce() {
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(10L)
                .build();
        String referenceId = UUID.randomUUID() + ":DECREASE";

        given(productRepository.findByIdForUpdate(productId)).willReturn(Optional.of(product));
        given(stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, referenceId, ProductStockMovement.MovementType.DECREASE))
                .willReturn(false, true);

        productService.decreaseStock(productId, 1, referenceId);
        productService.decreaseStock(productId, 1, referenceId);

        assertThat(product.getStockQuantity()).isEqualTo(9L);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 차감하려 하면 예외가 발생한다")
    void decreaseStock_fail_insufficientStock() {
        // given
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(10L)
                .build();
        given(productRepository.findByIdForUpdate(productId))
                .willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.decreaseStock(productId, 50, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Order 서비스의 주문 취소 요청으로 재고가 정상 복원된다")
    void restoreStock_success() {
        // given
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(70L)
                .build();
        given(productRepository.findByIdForUpdate(productId))
                .willReturn(Optional.of(product));

        // when
        productService.restoreStock(productId, 30, null);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(100L);
    }

    @Test
    @DisplayName("동일한 RESTORE referenceId로 두 번 복구해도 재고는 한 번만 복원된다")
    void restoreStock_sameReferenceIdTwice_restoresOnce() {
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(9L)
                .build();
        String referenceId = UUID.randomUUID() + ":RESTORE";

        given(productRepository.findByIdForUpdate(productId)).willReturn(Optional.of(product));
        given(stockMovementRepository.existsByProductIdAndReferenceIdAndMovementType(
                productId, referenceId, ProductStockMovement.MovementType.RESTORE))
                .willReturn(false, true);

        productService.restoreStock(productId, 1, referenceId);
        productService.restoreStock(productId, 1, referenceId);

        assertThat(product.getStockQuantity()).isEqualTo(10L);
    }

    @Test
    @DisplayName("재고 조정 수량이 0 이하이면 예외가 발생한다")
    void decreaseStock_fail_invalidQuantity() {
        assertThatThrownBy(() -> productService.decreaseStock(productId, 0, null))
                .isInstanceOf(BusinessException.class);

        verify(productRepository, times(0)).findByIdForUpdate(any());
    }
}
