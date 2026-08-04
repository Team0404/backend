package com.sparta.company.product.repository;

import com.sparta.company.product.entity.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    // 업체 삭제 시, 소속된 활성 상품들을 함께 논리 삭제하기 위한 조회
    List<Product> findAllByCompany_IdAndDeletedAtIsNull(UUID companyId);
}
