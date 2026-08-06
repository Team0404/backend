package com.sparta.company.company.dto.request;

import com.sparta.company.company.entity.CompanyType;

import java.util.UUID;

public record CompanySearchCondition(
        String keyword,
        CompanyType companyType,
        UUID hubId
) {
}
