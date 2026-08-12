package com.sparta.hub.dto.response;

import com.sparta.hub.entity.Hub;
import com.sparta.hub.entity.enums.HubStatus;
import com.sparta.hub.entity.enums.HubType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class HubResponse {
    private UUID id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private HubStatus status;
    private HubType hubType;

    public HubResponse(Hub hub) {
        this.id = hub.getHubId();
        this.name = hub.getName();
        this.address = hub.getAddress();
        this.latitude = hub.getLatitude();
        this.longitude = hub.getLongitude();
        this.status = hub.getStatus();
        this.hubType = hub.getHubType();
    }
}
