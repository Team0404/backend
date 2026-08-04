package com.sparta.hub.dto.request;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class HubUpdateRequest {

    private UUID id;    // 허브 id
    private String name;    // 허브 이름
    private String address; // 허브 주소
    private BigDecimal latitude;    // 위도
    private BigDecimal longitude;   // 경도
}
