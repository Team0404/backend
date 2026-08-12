package com.sparta.hub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class HubRouteUpdateRequest {

    @Positive(message = "예상 소요 시간은 0보다 커야 합니다.")
    private Integer durationMinutes;

    @DecimalMin(value = "0.0", inclusive = false,
            message = "이동 거리는 0보다 커야 합니다.")
    private BigDecimal distanceKm;
}
