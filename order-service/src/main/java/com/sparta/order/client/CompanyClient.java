package com.sparta.order.client;

import com.sparta.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "company-service", contextId = "companyClient", path = "/api/v1/companies")
public interface CompanyClient {

    @GetMapping("/{id}")
    ApiResponse<Map<String, Object>> getCompany(@PathVariable("id") UUID companyId);
}
