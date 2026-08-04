package com.sparta.delivery.domain.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DeliveryManagerSearchResponseDto {
    private List<DeliveryManagerResponseDto> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
