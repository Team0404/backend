package com.sparta.company.company.dto.request;

import com.sparta.company.company.entity.CompanyType;

import java.util.UUID;

/**
 * PATCH 방식 부분 수정 요청. 보낸 필드만 반영, 나머지는 null로 두면 기존 값 유지.
 */
public record CompanyUpdateRequest(
        String name,
        CompanyType companyType,
        UUID hubId,
        String address
) {
}
