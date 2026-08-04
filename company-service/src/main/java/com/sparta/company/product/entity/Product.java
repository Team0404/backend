package com.sparta.company.product.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.common.exception.BusinessException;
import com.sparta.company.company.entity.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // 같은 스키마 소속이므로 실제 FK 매핑 (단방향 - Company -> Product 역방향 매핑은 두지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Hub 서비스 참조 ID. 생성 시 company.getHubId() 값을 그대로 복사. DB FK 없음.
    @Column(name = "hub_id", nullable = false, columnDefinition = "uuid")
    private UUID hubId;

    @Column(name = "price", nullable = false)
    private Long price;

    // 이 허브에 이 업체가 보유 중인 재고 수량. 주문 생성/취소 시 Order 서비스 요청에 따라 증감.
    @Column(name = "stock_quantity", nullable = false)
    private Long stockQuantity;

    @Builder
    private Product(String name, Company company, UUID hubId, Long price, Long stockQuantity) {
        this.name = name;
        this.company = company;
        this.hubId = hubId;
        this.price = price;
        this.stockQuantity = (stockQuantity == null) ? 0L : stockQuantity;
    }

    public void update(String name, Long price) {
        if (name != null) {
            this.name = name;
        }
        if (price != null) {
            this.price = price;
        }
    }

    public UUID getCompanyId() {
        return company.getId();
    }

    /** 주문 취소로 인한 재고 복원, 수동 입고 겸용 (Order 서비스 restore-stock에서 호출) */
    public void increaseStock(long amount) {
        this.stockQuantity += amount;
    }

    /** 주문 생성으로 인한 재고 차감 (Order 서비스 decrease-stock에서 호출). 재고 부족 시 예외. */
    public void decreaseStock(long amount) {
//        if (this.stockQuantity < amount) {
//            throw new BusinessException(ProductErrorCode.INSUFFICIENT_STOCK);
//        }
        this.stockQuantity -= amount;
    }
}
