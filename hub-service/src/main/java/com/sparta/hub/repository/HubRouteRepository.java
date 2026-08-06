package com.sparta.hub.repository;

import com.sparta.hub.entity.HubRoute;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HubRouteRepository extends JpaRepository<HubRoute, UUID> {

    List<HubRoute> findAllByDeletedAtIsNull();
    Optional<HubRoute> findByIdAndDeletedAtIsNull(UUID id);


    //
    @Query("""
        SELECT hr
        FROM HubRoute hr
        JOIN FETCH hr.departureHub dh
        JOIN FETCH hr.arrivalHub ah
        WHERE dh.id = :departureHubId
        AND ah.id = :arrivalHubId
        AND hr.deletedAt IS NULL
        AND dh.deletedAt IS NULL
        AND ah.deletedAt IS NULL
""")
    Optional<HubRoute> findAllActiveRoutes(
            @Param("departureHubId") UUID departureHubId,
            @Param("arrivalHubId") UUID arrivalHubId);

}
