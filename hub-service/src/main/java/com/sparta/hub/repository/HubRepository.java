package com.sparta.hub.repository;

import com.sparta.hub.entity.Hub;
import com.sparta.hub.entity.HubRoute;
import com.sparta.hub.entity.enums.HubStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRepository extends JpaRepository<Hub, UUID> {

    // 허브 전체 조회
    List<Hub> findAllByDeletedAtIsNull();

    // Id 값으로 단건 조회
    Optional<Hub> findByHubIdAndDeletedAtIsNull(UUID id);

    // "신규 배송 경로"에 사용 할 수 있는 허브만 조회
    List<Hub> findAllByStatusAndDeletedAtIsNull(HubStatus status);




}
