package com.sparta.company.company.service;

import com.sparta.common.entity.UserRole;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.response.ApiResponse;
import com.sparta.common.security.UserPrincipal;
import com.sparta.company.client.hub.HubClient;
import com.sparta.company.client.hub.HubResponse;
import com.sparta.company.client.user.UserClient;
import com.sparta.company.client.user.UserResponse;
import com.sparta.company.company.dto.request.CompanyCreateRequest;
import com.sparta.company.company.dto.request.CompanyUpdateRequest;
import com.sparta.company.company.dto.response.CompanyResponse;
import com.sparta.company.company.entity.Company;
import com.sparta.company.company.entity.CompanyType;
import com.sparta.company.company.repository.CompanyQueryRepository;
import com.sparta.company.company.repository.CompanyRepository;
import com.sparta.company.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 업체(Company) CRUD 서비스 계층 단위 테스트.
 * Repository/FeignClient는 전부 mock 처리해서 DB, Eureka 없이 빠르게 실행된다.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyQueryRepository companyQueryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private HubClient hubClient;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private CompanyService companyService;

    private UUID hubId;
    private UUID companyId;
    private UserPrincipal masterUser;

    @BeforeEach
    void setUp() {
        hubId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        masterUser = new UserPrincipal(UUID.randomUUID(), "master", UserRole.MASTER);
    }

    @Test
    @DisplayName("MASTER는 존재하는 hubId로 업체를 생성할 수 있다")
    void create_success() {
        // given
        CompanyCreateRequest request = new CompanyCreateRequest(
                "일산 건조식품 가공업체", CompanyType.PRODUCER, hubId, "경기도 고양시 일산동구 ...");

        given(hubClient.getHub(hubId))
                .willReturn(ApiResponse.success(
                        new HubResponse(hubId, "경기 북부 센터", "경기도 고양시...")
                        ));

        Company saved = Company.builder()
                .name(request.name())
                .companyType(request.companyType())
                .hubId(request.hubId())
                .address(request.address())
                .build();
        given(companyRepository.save(any(Company.class))).willReturn(saved);

        // when
        CompanyResponse response = companyService.create(request, masterUser);

        // then
        assertThat(response.name()).isEqualTo("일산 건조식품 가공업체");
        assertThat(response.hubId()).isEqualTo(hubId);
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    @DisplayName("존재하지 않는 hubId로 생성하면 예외가 발생한다")
    void create_fail_invalidHub() {
        // given
        CompanyCreateRequest request = new CompanyCreateRequest(
                "테스트 업체", CompanyType.RECEIVER, hubId, "주소");

        given(hubClient.getHub(any()))
                .willThrow(mock(feign.FeignException.NotFound.class));

        // when & then
        assertThatThrownBy(() -> companyService.create(request, masterUser))
                .isInstanceOf(BusinessException.class);

        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("업체 단건 조회에 성공한다")
    void getOne_success() {
        // given
        Company company = Company.builder()
                .name("부산 수산물 도매업체")
                .companyType(CompanyType.RECEIVER)
                .hubId(hubId)
                .address("부산 동구 ...")
                .build();
        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

        // when
        CompanyResponse response = companyService.getOne(companyId);

        // then
        assertThat(response.name()).isEqualTo("부산 수산물 도매업체");
    }

    @Test
    @DisplayName("삭제된(또는 존재하지 않는) 업체 조회 시 예외가 발생한다")
    void getOne_fail_notFound() {
        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getOne(companyId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("MASTER는 업체 정보를 수정할 수 있다")
    void update_success_master() {
        // given
        Company company = Company.builder()
                .name("수정 전 이름")
                .companyType(CompanyType.PRODUCER)
                .hubId(hubId)
                .address("수정 전 주소")
                .build();
        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

        CompanyUpdateRequest request = new CompanyUpdateRequest("수정 후 이름", null, null, null);

        // when
        CompanyResponse response = companyService.update(companyId, request, masterUser);

        // then
        assertThat(response.name()).isEqualTo("수정 후 이름");
    }

    @Test
    @DisplayName("SUPPLIER_MANAGER가 본인 업체가 아닌 다른 업체를 수정하면 예외가 발생한다")
    void update_fail_forbidden_otherCompany() {
        // given
        UserPrincipal supplierManager = new UserPrincipal(UUID.randomUUID(), "supplier", UserRole.SUPPLIER_MANAGER);

        Company company = Company.builder()
                .name("남의 업체")
                .companyType(CompanyType.PRODUCER)
                .hubId(hubId)
                .address("주소")
                .build();
        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));

        // 본인 소속 업체 ID가 지금 수정하려는 companyId와 다름
        given(userClient.getUser(supplierManager.getUserId().toString()))
                .willReturn(new UserResponse(
                        supplierManager.getUserId(), "supplier", "SUPPLIER_MANAGER", null, UUID.randomUUID()));

        CompanyUpdateRequest request = new CompanyUpdateRequest("바꾸려는 이름", null, null, null);

        // when & then
        assertThatThrownBy(() -> companyService.update(companyId, request, supplierManager))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("MASTER가 업체를 삭제하면 논리 삭제되고, 소속 상품도 함께 논리 삭제된다")
    void delete_success_cascadesToProducts() {
        // given
        Company company = Company.builder()
                .name("삭제될 업체")
                .companyType(CompanyType.PRODUCER)
                .hubId(hubId)
                .address("주소")
                .build();
        given(companyRepository.findByIdAndDeletedAtIsNull(companyId))
                .willReturn(Optional.of(company));
        given(productRepository.findAllByCompany_IdAndDeletedAtIsNull(companyId))
                .willReturn(List.of());

        // when
        companyService.delete(companyId, masterUser);

        // then
        assertThat(company.isDeleted()).isTrue();
        verify(productRepository, times(1)).findAllByCompany_IdAndDeletedAtIsNull(companyId);
    }
}