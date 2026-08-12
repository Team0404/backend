package com.sparta.company.company.entity;

import com.sparta.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "p_company")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 20)
    private CompanyType companyType;

    // Hub 서비스 참조 ID. DB FK 제약 없음 - HubClient로 존재 검증 후 저장.
    @Column(name = "hub_id", nullable = false, columnDefinition = "uuid")
    private UUID hubId;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Builder
    private Company(String name, CompanyType companyType, UUID hubId, String address) {
        this.name = name;
        this.companyType = companyType;
        this.hubId = hubId;
        this.address = address;
    }

    /**
     * 부분 수정. null인 필드는 기존 값 유지.
     */
    public void update(String name, CompanyType companyType, UUID hubId, String address) {
        if (name != null) {
            this.name = name;
        }
        if (companyType != null) {
            this.companyType = companyType;
        }
        if (hubId != null) {
            this.hubId = hubId;
        }
        if (address != null) {
            this.address = address;
        }
    }
}
