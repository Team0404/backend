package com.sparta.hub.entity;

import com.sparta.common.entity.BaseEntity;
import com.sparta.hub.dto.request.HubUpdateRequest;
import com.sparta.hub.entity.enums.HubStatus;
import com.sparta.hub.entity.enums.HubType;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HubStatus status = HubStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "hub_type", nullable = false)
    private HubType hubType;

    public Hub( String name, String address, BigDecimal latitude, BigDecimal longitude,
                HubType hubType) {

        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hubType = hubType;
        this.status = HubStatus.ACTIVE;
    }

    public void update(HubUpdateRequest request) {
        this.name = request.getName();
        this.address = request.getAddress();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
    }


    // 신규 배송 차단
    public void softDeactivation() {
        this.status = HubStatus.DEACTIVATING;
    }
    // 기존 배송 완료 후 최종 삭제
    @Override
    public void softDelete(UUID deletedBy) {
        this.status = HubStatus.INACTIVE;
        super.softDelete(deletedBy);
    }
}
