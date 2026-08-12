package com.sparta.company.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI companyServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Company / Product Service API")
                        .description("업체(Company) / 상품(Product) 관리 API")
                        .version("v1"));
    }
}
