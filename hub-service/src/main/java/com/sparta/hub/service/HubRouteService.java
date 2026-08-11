package com.sparta.hub.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.hub.dto.request.HubRouteCreateRequest;
import com.sparta.hub.dto.response.HubRoutePathResponse;
import com.sparta.hub.dto.response.HubRouteResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.entity.HubRoute;
import com.sparta.hub.exception.HubErrorCode;
import com.sparta.hub.exception.HubRouteErrorCode;
import com.sparta.hub.repository.HubRepository;
import com.sparta.hub.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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


        return hubRouteRepository.findAll().stream()
                .map(HubRouteResponse::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // P2P HubRoutePath
    @Cacheable(cacheNames = "directRoutes",
            key = "#departureHubId.toString() + ':' + #arrivalHubId.toString()")
    @Transactional(readOnly = true)
    public HubRouteResponse findRoute( UUID departureHubId,
                                       UUID arrivalHubId){
        if(departureHubId.equals(arrivalHubId)) {
            throw new BusinessException(HubRouteErrorCode.SAME_DEPARTURE_AND_ARRIVAL_HUB);

        }

        HubRoute route = hubRouteRepository.findAllActiveRoutes(departureHubId,arrivalHubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));


        return new HubRouteResponse(route);
    }

    // 허브 출발,도착 경로 조회
    @Cacheable(cacheNames = "routePaths",
    key = "#departureHubId.toString() + ':' + #arrivalHubId.toString()")
    @Transactional(readOnly = true)
    public HubRoutePathResponse findPath(UUID departureHubId,
                                         UUID arrivalHubId) {
        if(departureHubId.equals(arrivalHubId)){
            throw new BusinessException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND);
        }

        HubRoute hubRoute =
                hubRouteRepository.findAllActiveRoutes(departureHubId,arrivalHubId)
                        .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        HubRouteResponse routeResponse =
                new HubRouteResponse(hubRoute);

        return new HubRoutePathResponse(
                hubRoute.getDepartureHub().getHubId(),
                hubRoute.getDepartureHub().getName(),
                hubRoute.getArrivalHub().getHubId(),
                hubRoute.getArrivalHub().getName(),
                List.of(routeResponse),
                hubRoute.getDurationMinutes(),
                hubRoute.getDistanceKm()
        );

    }





    // 허브 이동 정보 생성
    public HubRouteResponse createHubRoute(HubRouteCreateRequest request){
        Hub departureHubId = hubRepository.findByHubIdAndDeletedAtIsNull
                (request.getDepartureHubId()).
                orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));


        Hub arrivalHubId = hubRepository.findByHubIdAndDeletedAtIsNull(request.getArrivalHubId()).
                orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

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
    @Transactional
    public void hubRouteUpdate(UUID routeId){
        HubRoute hubRoute = hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(routeId)
                .orElseThrow(() -> new BusinessException(HubRouteErrorCode.HUB_ROUTE_NOT_FOUND));

        hubRoute.updateRoute(hubRoute.getDurationMinutes(),hubRoute.getDistanceKm());

    }

    // 허브 라우터 삭제
    @Transactional
    public void deleteHubRoute(UUID id){
        HubRoute hubroute = hubRouteRepository.findByHubRouteIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않은 라우터 입니다."));

        hubroute.softDelete(id);

    }






}
