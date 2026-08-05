package com.sparta.hub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class HubCreateRequest {

    @Schema(description = "허브 이름", example = "서울특별시 센터")
    @NotBlank(message = "허브 이름은 필수입니다.")
    private String name;

    @Schema(description = "허브 주소", example = "서울특별시 송파구 송파대로 55")
    @NotBlank(message = "허브 주소는 필수입니다.")
    private String address;

    @Schema(description = "허브 위도", example = "37.4741000")
    @NotNull(message = "위도는 필수입니다.")
    private BigDecimal latitude;

    @Schema(description = "허브 경도", example = "127.1233000")
    @NotNull(message = "경도는 필수입니다.")
    private BigDecimal longitude;


}
