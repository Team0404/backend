package com.sparta.hub.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.hub.dto.request.HubRouteCreateRequest;
import com.sparta.hub.dto.request.HubRouteUpdateRequest;
import com.sparta.hub.dto.response.HubRoutePathResponse;
import com.sparta.hub.dto.response.HubRouteResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.entity.HubRoute;
import com.sparta.hub.entity.enums.HubStatus;
import com.sparta.hub.exception.HubErrorCode;
import com.sparta.hub.exception.HubRouteErrorCode;
import com.sparta.hub.repository.HubRepository;
import com.sparta.hub.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HubRouteService {
    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;


    // 허브 이동 단건 조회
    @Cacheable(
            cacheNames = "hubRoutes",
            key = "#routeId"
    )
    @Transactional(readOnly = true)
    public HubRouteResponse findHubRoute(UUID routeId){
        HubRoute hubRoute = hubRouteRepository.
                findById(routeId).
                orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        return new HubRouteResponse(hubRoute);
    }

    // 허브 이동 전체 조회
    @Cacheable(cacheNames = "hubRouteList",key = "'all'")
    @Transactional(readOnly = true)
    public List<HubRouteResponse> findAllHubRoutes(){


        return hubRouteRepository.findAllByDeletedAtIsNull().stream()
                .map(HubRouteResponse::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // 허브 출발,도착 경로 조회
    @Cacheable(cacheNames = "routePaths",
    key = "#departureHubId.toString() + ':' + #arrivalHubId.toString()")
    @Transactional(readOnly = true)
    public HubRoutePathResponse findPath(UUID departureHubId,
                                         UUID arrivalHubId) {
        if (departureHubId.equals(arrivalHubId)) {
            throw new BusinessException(
                    HubRouteErrorCode.SAME_DEPARTURE_AND_ARRIVAL_HUB
            );
        }

        Hub departureHub =
                hubRepository.findByHubIdAndStatusAndDeletedAtIsNull(
                                departureHubId,
                                HubStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        HubRouteErrorCode.DEPARTURE_HUB_INACTIVE
                                ));

        Hub arrivalHub =
                hubRepository.findByHubIdAndStatusAndDeletedAtIsNull(
                                arrivalHubId,
                                HubStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        HubRouteErrorCode.ARRIVAL_HUB_INACTIVE
                                ));

        List<HubRoute> routes =
                findRoutesByBfs(departureHubId, arrivalHubId);

        List<HubRouteResponse> routeResponses = routes.stream()
                .map(HubRouteResponse::new)
                .collect(Collectors.toCollection(ArrayList::new));

        int totalDurationMinutes = routes.stream()
                .mapToInt(HubRoute::getDurationMinutes)
                .sum();

        BigDecimal totalDistanceKm = routes.stream()
                .map(HubRoute::getDistanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new HubRoutePathResponse(
                departureHub.getHubId(),
                departureHub.getName(),
                arrivalHub.getHubId(),
                arrivalHub.getName(),
                routeResponses,
                totalDurationMinutes,
                totalDistanceKm
        );

    }

    // Hub and Spoke 방식
    private List<HubRoute> findRoutesByBfs(
            UUID departureHubId,
            UUID arrivalHubId
    ) {
        List<HubRoute> allRoutes =
                hubRouteRepository.findAllAvailableRoutes();

        // 출발 허브 ID를 기준으로 연결된 경로 분류
        Map<UUID, List<HubRoute>> graph = allRoutes.stream()
                .collect(Collectors.groupingBy(
                        route -> route.getDepartureHub().getHubId()
                ));

        Queue<UUID> queue = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();

        // 특정 허브까지 어떤 경로를 통해 왔는지 저장
        Map<UUID, HubRoute> previousRoutes = new HashMap<>();

        queue.offer(departureHubId);
        visited.add(departureHubId);

        while (!queue.isEmpty()) {
            UUID currentHubId = queue.poll();

            if (currentHubId.equals(arrivalHubId)) {
                break;
            }

            List<HubRoute> connectedRoutes =
                    graph.getOrDefault(currentHubId, List.of());

            for (HubRoute route : connectedRoutes) {
                UUID nextHubId =
                        route.getArrivalHub().getHubId();

                if (visited.add(nextHubId)) {
                    previousRoutes.put(nextHubId, route);
                    queue.offer(nextHubId);
                }
            }
        }

        if (!visited.contains(arrivalHubId)) {
            throw new BusinessException(
                    HubRouteErrorCode.HUB_ROUTE_PATH_NOT_FOUND
            );
        }

        // 도착 허브부터 출발 허브까지 역으로 경로 복원
        List<HubRoute> routes = new ArrayList<>();
        UUID currentHubId = arrivalHubId;

        while (!currentHubId.equals(departureHubId)) {
            HubRoute route = previousRoutes.get(currentHubId);

            if (route == null) {
                throw new BusinessException(
                        HubRouteErrorCode.HUB_ROUTE_PATH_NOT_FOUND
                );
            }

            routes.add(route);
            currentHubId = route.getDepartureHub().getHubId();
        }

        Collections.reverse(routes);

        return routes;
    }





    // 허브 이동 정보 생성
    @Caching(evict = {
            @CacheEvict(cacheNames = "hubRoutes", allEntries = true),
            @CacheEvict(cacheNames = "hubRouteList", allEntries = true),
            @CacheEvict(cacheNames = "directRoutes", allEntries = true),
            @CacheEvict(cacheNames = "routePaths", allEntries = true)
    })
    @Transactional
    public HubRouteResponse createHubRoute(HubRouteCreateRequest request){
        Hub departureHubId =
                hubRepository.findByHubIdAndStatusAndDeletedAtIsNull(
                                request.getDepartureHubId(),
                                HubStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        HubRouteErrorCode.DEPARTURE_HUB_INACTIVE
                                ));

        Hub arrivalHubId =
                hubRepository.findByHubIdAndStatusAndDeletedAtIsNull(
                                request.getArrivalHubId(),
                                HubStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        HubRouteErrorCode.ARRIVAL_HUB_INACTIVE
                                ));



        if(departureHubId.getHubId().equals(arrivalHubId.getHubId())){
            throw new IllegalArgumentException(
                    "출발 허브와 도착 허브는 같을 수 없습니다."
            );
        }

        HubRoute route = new HubRoute(departureHubId,arrivalHubId,
                request.getDurationMinutes(),request.getDistanceKm());

        hubRouteRepository.save(route);

        return new HubRouteResponse(route);

    }

    // 허브 라우터 수정
    @Caching(evict = {
            @CacheEvict(cacheNames = "hubRoutes", allEntries = true),
            @CacheEvict(cacheNames = "hubRouteList", allEntries = true),
            @CacheEvict(cacheNames = "directRoutes", allEntries = true),
            @CacheEvict(cacheNames = "routePaths", allEntries = true)
    })
    @Transactional
    public void hubRouteUpdate(UUID routeId, HubRouteUpdateRequest request){
        HubRoute route =
                hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(routeId)
                        .orElseThrow(() ->
                                new BusinessException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));

        route.updateRoute(request.getDurationMinutes(), request.getDistanceKm());

    }

    // 허브 라우터 삭제
    @Caching(evict = {
            @CacheEvict(cacheNames = "hubRoutes", allEntries = true),
            @CacheEvict(cacheNames = "hubRouteList", allEntries = true),
            @CacheEvict(cacheNames = "directRoutes", allEntries = true),
            @CacheEvict(cacheNames = "routePaths", allEntries = true)
    })
    @Transactional
    public void deleteHubRoute(UUID routeId, UUID deletedBy){
        HubRoute route =
                hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(routeId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        HubRouteErrorCode.HUB_ROUTE_NOT_FOUND
                                ));

        route.softDelete(deletedBy);

    }






}
