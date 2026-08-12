package com.sparta.company.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record ProductCreateRequest(

    @NotBlank(message = "상품명은 필수입니다.")
    String name,

    @NotNull(message = "소속 업체 ID는 필수입니다.")
    UUID companyId,

    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    Long price,

    @PositiveOrZero(message = "초기 재고 수량은 0 이상이어야 합니다.")
    Long initialStock // 생략시 0으로 시작
){
}
