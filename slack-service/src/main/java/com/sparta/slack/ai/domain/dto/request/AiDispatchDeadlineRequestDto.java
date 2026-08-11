package com.sparta.slack.ai.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AiDispatchDeadlineRequestDto {

    @NotNull
    private UUID orderId;

    @NotNull
    private UUID deliveryId;

    @NotNull
    @NotBlank
    private String productInfo;

    private String requestNote;

    @NotNull
    @NotBlank
    private String originHubName;

    private List<String> waypointHubNames;

    @NotNull
    @NotBlank
    private String destAddress;

    @NotNull
    @NotBlank
    private String managerSlackId;

    /** 배송 담당자 근무시간. 미입력 시 서비스에서 기본값(09-18) 적용 */
    private String workingHours;
}
