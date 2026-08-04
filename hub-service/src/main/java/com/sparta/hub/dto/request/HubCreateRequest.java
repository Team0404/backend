package com.sparta.hub.dto.request;

import jakarta.persistence.Column;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class HubCreateRequest {

    private String name;    // 허브 이름
    private String address; // 허브 주소
    private BigDecimal latitude;    // 위도
    private BigDecimal longitude;   // 경도



}
