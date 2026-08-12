package com.sparta.delivery.domain.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DeliveryCancelResponseDto {
    private UUID deliveryId;
    private String status;
    private LocalDateTime cancelledAt;
    private Integer cancelledRouteCount;
    private Integer preservedRouteCount;
}
