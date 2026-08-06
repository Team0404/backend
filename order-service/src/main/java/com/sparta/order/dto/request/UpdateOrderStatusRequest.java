package com.sparta.order.dto.request;

import com.sparta.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(

        @NotNull(message = "변경할 주문 상태는 필수입니다.")
        OrderStatus status
) {
}
