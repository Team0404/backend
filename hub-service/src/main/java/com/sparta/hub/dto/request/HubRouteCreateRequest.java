package com.sparta.hub.dto.request;

import com.sparta.hub.entity.Hub;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class HubRouteCreateRequest {


    @NotNull(message = "출발 허브 ID는 필수입니다.")
    private UUID departureHubId;

    @NotNull(message = "도착 허브 ID는 필수입니다.")
    private UUID arrivalHubId;

    @NotNull(message = "예상 소요 시간은 필수입니다.")
    @Positive(message = "예상 소요 시간은 0보다 커야 합니다.")
    private Integer durationMinutes;

    @NotNull(message = "이동 거리는 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "이동 거리는 0보다 커야 합니다.")
    private BigDecimal distanceKm;
}
