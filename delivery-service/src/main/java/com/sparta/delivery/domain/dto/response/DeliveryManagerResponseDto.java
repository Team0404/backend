package com.sparta.delivery.domain.dto.response;

import com.sparta.delivery.domain.entity.DeliveryManagerType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryManagerResponseDto {
    private UUID userId;
    private DeliveryManagerType type;
    private UUID hubId;          // HUB 는 null
    private Integer sequence;
}
