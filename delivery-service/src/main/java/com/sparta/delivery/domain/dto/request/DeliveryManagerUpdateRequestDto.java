package com.sparta.delivery.domain.dto.request;

import com.sparta.delivery.domain.entity.DeliveryManagerType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryManagerUpdateRequestDto {

    private DeliveryManagerType type;   // 선택
    private UUID hubId;                  // COMPANY 필수·존재확인 / HUB null 처리
    private Integer sequence;            // 배송 순번 재지정(선택)
}
