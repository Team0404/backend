package com.sparta.hub.service;

import com.sparta.hub.dto.request.HubRouteCreateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.dto.response.HubRoutePathResponse;
import com.sparta.hub.dto.response.HubRouteResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.entity.HubRoute;
import com.sparta.hub.repository.HubRepository;
import com.sparta.hub.repository.HubRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubRouteService {
    private final HubRouteRepository hubRouteRepository;
    private final HubRepository hubRepository;

    // 허브 이동 단건 조회
    @Transactional(readOnly = true)
    public HubRouteResponse findHubRoute(UUID routeId){
        HubRoute hubRoute = hubRouteRepository.
                findById(routeId).
                orElseThrow(() -> new RuntimeException("존재하지 않는 허브 이동 경로입니다."));

        return new HubRouteResponse(hubRoute);
    }

    // 허브 이동 전체 조회
    @Transactional(readOnly = true)
    public List<HubRouteResponse> findAllHubRoutes(){


        return hubRouteRepository.findAll().stream()
                .map(HubRouteResponse::new)
                .toList();
    }

    // P2P HubRoutePath
    @Transactional(readOnly = true)
    public HubRouteResponse findRoute( UUID departureHubId,
                                       UUID arrivalHubId){
        if(departureHubId.equals(arrivalHubId)) {
            throw new IllegalArgumentException(
                    "출발 허브와 도착 허브는 같을 수 없습니다."
            );
        }

        HubRoute route = hubRouteRepository.findAllActiveRoutes(departureHubId,arrivalHubId)
                .orElseThrow(() -> new RuntimeException("등록된 이동 경로가 없습니다."));


        return new HubRouteResponse(route);
    }

    // 허브 출발,도착 경로 조회
    @Transactional(readOnly = true)
    public HubRoutePathResponse findPath(UUID departureHubId,
                                         UUID arrivalHubId) {
        if(departureHubId.equals(arrivalHubId)){
            throw new RuntimeException("출발 허브와 도착 허브는 같을 수 없습니다.");
        }

        HubRoute hubRoute =
                hubRouteRepository.findAllActiveRoutes(departureHubId,arrivalHubId)
                        .orElseThrow(() -> new RuntimeException("등록된 이동경로가 없습니다."));

        HubRouteResponse routeResponse =
                new HubRouteResponse(hubRoute);

        return new HubRoutePathResponse(
                hubRoute.getDepartureHub().getId(),
                hubRoute.getDepartureHub().getName(),
                hubRoute.getArrivalHub().getId(),
                hubRoute.getArrivalHub().getName(),
                List.of(routeResponse),
                hubRoute.getDurationMinutes(),
                hubRoute.getDistanceKm()
        );

    }





    // 허브 이동 정보 생성
    public void createHubRoute(HubRouteCreateRequest request){
        Hub departureHubId = hubRepository.findByIdAndDeletedAtIsNull
                (request.getDepartureHubId()).
                orElseThrow(() -> new RuntimeException("출발 허브 Id가 존재 하지 않습니다."));


        Hub arrivalHubId = hubRepository.findByIdAndDeletedAtIsNull(request.getDepartureHubId()).
                orElseThrow(() -> new RuntimeException("도착 허브 Id가 존재 하지 않습니다."));

        if(departureHubId.getId() == arrivalHubId.getId()){
            throw new IllegalArgumentException(
                    "출발 허브와 도착 허브는 같을 수 없습니다."
            );
        }

        HubRoute route = new HubRoute(departureHubId,arrivalHubId,
                request.getDurationMinutes(),request.getDistanceKm());

        hubRouteRepository.save(route);

    }

    // 허브 라우터 수정
    @Transactional
    public void hubRouteUpdate(UUID routeId){
        HubRoute hubRoute = hubRouteRepository.findByIdAndDeletedAtIsNull(routeId)
                .orElseThrow(() -> new RuntimeException("허브라우터가 존재하지 않습니다."));

        hubRoute.updateRoute(hubRoute.getDurationMinutes(),hubRoute.getDistanceKm());

    }

    // 허브 라우터 삭제
    @Transactional
    public void deleteHubRoute(UUID id){
        HubRoute hubroute = hubRouteRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않은 라우터 입니다."));

        hubroute.softDelete(id);

    }






}
