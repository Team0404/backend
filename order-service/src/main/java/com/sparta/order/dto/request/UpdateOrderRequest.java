package com.sparta.order.dto.request;

import com.sparta.order.entity.OrderStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateOrderRequest(

        @Size(max = 500, message = "요청사항은 500자 이하여야 합니다.")
        String requestNote,

        @FutureOrPresent(message = "배송 마감일은 현재 이후여야 합니다.")
        LocalDateTime deliveryDeadline,

        OrderStatus status
) {
}
