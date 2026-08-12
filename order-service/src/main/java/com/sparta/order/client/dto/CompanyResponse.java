package com.sparta.order.client.dto;

import java.util.UUID;

public record CompanyResponse(
        UUID companyId,
        String name,
        UUID hubId,
        String address
) {
}