package com.sparta.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "수령 업체 ID는 필수입니다.")
        UUID companyId,

        @NotNull(message = "주문 상품 목록은 필수입니다.")
        @Size(min = 1, message = "주문 상품은 최소 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemRequest> orderItems,

        @Size(max = 500, message = "요청사항은 500자 이하여야 합니다.")
        String requestNote,

        @FutureOrPresent(message = "배송 마감일은 현재 이후여야 합니다.")
        LocalDateTime deliveryDeadline
) {
    public record OrderItemRequest(

            @NotNull(message = "상품 ID는 필수입니다.")
            UUID productId,

            @NotNull(message = "수량은 필수입니다.")
            @jakarta.validation.constraints.Min(value = 1, message = "수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {
    }
}
