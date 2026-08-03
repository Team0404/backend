package com.sparta.delivery.domain.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public class DeliverySearchResponseDto {
    private List<DeliverySummaryResponseDto> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
