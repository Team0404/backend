package com.sparta.hub.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.hub.dto.request.HubUpdateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_hubs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hub extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID hubId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10,scale = 7)
    private BigDecimal latitude;    // 위도
    @Column(name = "longitude", nullable = false ,precision = 10,scale = 7)
    private BigDecimal longitude;   // 경도

    public Hub( String name, String address, BigDecimal latitude, BigDecimal longitude) {

        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void update(HubUpdateRequest request) {
        this.name = request.getName();
        this.address = request.getAddress();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
    }


}
