package com.sparta.company.product.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.client.user.UserClient;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.entity.CompanyType;
import com.sparta.company.company.repository.CompanyRepository;
import com.sparta.company.product.dto.request.ProductCreateRequest;
import com.sparta.company.product.dto.request.ProductUpdateRequest;
import com.sparta.company.product.dto.response.ProductResponse;
import com.sparta.company.product.entity.Product;
import com.sparta.company.product.repository.ProductQueryRepository;
import com.sparta.company.product.repository.ProductRepository;
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
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductQueryRepository productQueryRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserClient userClient;

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
        assertThat(response.hubId()).isEqualTo(hubId); // company.hubId와 동일해야 함
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
        assertThat(response.price()).isEqualTo(1000L); // price는 null로 보냈으니 유지
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
    @DisplayName("Order 서비스의 주문 생성 요청으로 재고가 정상 차감된다")
    void decreaseStock_success() {
        // given
        Product product = Product.builder()
                .name("마른오징어 가공품")
                .company(company)
                .hubId(hubId)
                .price(15000L)
                .stockQuantity(100L)
                .build();
        given(productRepository.findByIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(product));

        // when
        productService.decreaseStock(productId, 30);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(70L);
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
        given(productRepository.findByIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> productService.decreaseStock(productId, 50))
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
        given(productRepository.findByIdAndDeletedAtIsNull(productId))
                .willReturn(Optional.of(product));

        // when
        productService.restoreStock(productId, 30);

        // then
        assertThat(product.getStockQuantity()).isEqualTo(100L);
    }

    @Test
    @DisplayName("재고 조정 수량이 0 이하이면 예외가 발생한다")
    void decreaseStock_fail_invalidQuantity() {
        assertThatThrownBy(() -> productService.decreaseStock(productId, 0))
                .isInstanceOf(BusinessException.class);

        verify(productRepository, times(0)).findByIdAndDeletedAtIsNull(any());
    }
}
