package com.sparta.delivery.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DeliveryCreateRequestDto {
    @NotNull
    private UUID orderId;
    @NotNull
    private UUID originHubId;
    @NotNull
    private UUID destHubId;
    @NotNull
    @NotBlank
    private String deliveryAddress;
    @NotNull
    @NotBlank
    private String recipientName;
    @NotNull
    @NotBlank
    private String recipientSlackId;

    /**
     * 상품 및 수량 정보 (예: "마른 오징어 50개").
     * 배송 도메인이 쓰는 값은 아니고, AI 발송시한 산출(A1) 프롬프트에 그대로 전달하기 위해
     * 주문 서비스가 함께 실어 보낸다. 배송이 주문을 역으로 조회하면 아직 커밋되지 않은
     * 주문을 읽게 되므로(순환 호출) push 방식으로 받는다.
     */
    private String productInfo;

    /** 주문 요청사항 (납기 일자/시간 등). productInfo 와 동일한 이유로 전달받는다. */
    private String requestNote;
}
