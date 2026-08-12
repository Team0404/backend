package com.sparta.company.product.repository;

import com.sparta.company.product.entity.Product;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);

    // 업체 삭제 시, 소속된 활성 상품들을 함께 논리 삭제하기 위한 조회
    List<Product> findAllByCompany_IdAndDeletedAtIsNull(UUID companyId);

    /**
     * 재고 증감(decrease/restore-stock) 전용 조회. Postgres 행 잠금(SELECT ... FOR UPDATE)을 걸어서
     * 같은 상품에 대한 동시 재고 변경 요청이 순차적으로 처리되도록 강제한다 (Lost Update 방지).
     *
     * 인스턴스가 여러 개로 수평 확장돼도, 실제 잠금은 DB(공유 자원) 레벨에서 걸리므로
     * 애플리케이션 인스턴스 개수와 무관하게 정합성이 보장된다.
     *
     * 잠금을 오래 기다리지 않도록 3초 타임아웃을 걸어뒀다. 타임아웃 시
     * PessimisticLockException/LockTimeoutException이 발생하며, 서비스 계층에서
     * BusinessException(INSUFFICIENT_STOCK과 별개의 재시도 유도 에러)으로 변환한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("select p from Product p where p.id = :id and p.deletedAt is null")
    Optional<Product> findByIdForUpdate(@Param("id") UUID id);
}
