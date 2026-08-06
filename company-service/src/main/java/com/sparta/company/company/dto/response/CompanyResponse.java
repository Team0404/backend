package com.sparta.company.company.dto.response;

import com.sparta.company.company.entity.Company;
import com.sparta.company.company.entity.CompanyType;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID companyId,
        String name,
        CompanyType companyType,
        UUID hubId,
        String address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getCompanyType(),
                company.getHubId(),
                company.getAddress(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
