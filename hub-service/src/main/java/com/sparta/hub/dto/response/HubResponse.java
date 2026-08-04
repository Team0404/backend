package com.sparta.hub.dto.response;

import com.sparta.hub.entity.Hub;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class HubResponse {
    private UUID id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public HubResponse(Hub hub) {
        this.id = hub.getId();
        this.name = hub.getName();
        this.address = hub.getAddress();
        this.latitude = hub.getLatitude();
        this.longitude = hub.getLongitude();
    }
}
