package com.sparta.company.company.dto.request;

import com.sparta.company.company.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompanyCreateRequest(

        @NotBlank(message = "업체명은 필수입니다.")
        String name,

        @NotNull(message = "업체 타입은 필수입니다.")
        CompanyType companyType,

        @NotNull(message = "관리 허브 ID는 필수입니다.")
        UUID hubId,

        @NotBlank(message = "업체 주소는 필수입니다.")
        String address
) {
}
