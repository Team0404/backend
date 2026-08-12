package com.sparta.delivery.domain.dto.request;

import com.sparta.delivery.domain.entity.DeliveryManagerType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryManagerCreateRequestDto {

    @NotNull
    private UUID userId;          // 배송담당자로 등록할 사용자 ID (사용자서비스 user_id)

    @NotNull
    private DeliveryManagerType type;   // HUB / COMPANY

    private UUID hubId;           // COMPANY 필수 / HUB 미입력 (서버에서 검증)
}
