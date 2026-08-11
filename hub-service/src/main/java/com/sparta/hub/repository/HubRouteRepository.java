package com.sparta.hub.repository;

import com.sparta.hub.entity.HubRoute;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    List<HubRoute> findAllByDeletedAtIsNull();
    Optional<HubRoute> findByHubRouteIdAndDeletedAtIsNull(UUID id);

    List<HubRoute>
    findAllByDepartureHub_HubIdOrArrivalHub_HubIdAndDeletedAtIsNull(
            UUID departureHubId,
            UUID arrivalHubId
    );


    //
    @Query("""
        SELECT hr
        FROM HubRoute hr
        JOIN FETCH hr.departureHub dh
        JOIN FETCH hr.arrivalHub ah
        WHERE dh.hubId = :departureHubId
        AND ah.hubId = :arrivalHubId
        AND dh.status = com.sparta.hub.entity.enums.HubStatus.ACTIVE
        AND ah.status = com.sparta.hub.entity.enums.HubStatus.ACTIVE
        AND hr.deletedAt IS NULL
        AND dh.deletedAt IS NULL
        AND ah.deletedAt IS NULL
        """)
    Optional<HubRoute> findAllActiveRoutes(
            @Param("departureHubId") UUID departureHubId,
            @Param("arrivalHubId") UUID arrivalHubId);

    // 허브 활성 상태 조회
    @Query("""
    SELECT hr
    FROM HubRoute hr
    WHERE (
        hr.departureHub.hubId = :hubId
        OR hr.arrivalHub.hubId = :hubId
    )
    AND hr.deletedAt IS NULL
        """)
    List<HubRoute> findAllActiveRoutesByHubId(
            @Param("hubId") UUID hubId
    );

    @Query("""
    SELECT hr
    FROM HubRoute hr
    JOIN FETCH hr.departureHub dh
    JOIN FETCH hr.arrivalHub ah
    WHERE hr.deletedAt IS NULL
      AND dh.deletedAt IS NULL
      AND ah.deletedAt IS NULL
      AND dh.status = com.sparta.hub.entity.enums.HubStatus.ACTIVE
      AND ah.status = com.sparta.hub.entity.enums.HubStatus.ACTIVE
""")
    List<HubRoute> findAllAvailableRoutes();





}


