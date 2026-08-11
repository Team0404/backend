package com.sparta.hub.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.hub.dto.request.HubCreateRequest;
import com.sparta.hub.dto.request.HubUpdateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.entity.HubRoute;
import com.sparta.hub.entity.enums.HubStatus;
import com.sparta.hub.exception.HubErrorCode;
import com.sparta.hub.repository.HubRepository;
import com.sparta.hub.repository.HubRouteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HubService {
    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;


    // 허브 단건 조회

    @Cacheable(
            cacheNames = "hubs",
            key = "#hubId"
    )
    @Transactional(readOnly = true)
    public HubResponse findHub(UUID hubId){
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId).orElseThrow(()
                -> new RuntimeException("없는 번호 입니다."));

        return new HubResponse(hub);

    }

    // 허브 전체 조회
    @Cacheable(
            cacheNames = "hubList",
            key = "'all'"
    )
    @Transactional(readOnly = true)
    public List<HubResponse> findAllHubs(){
        List<Hub> hub = hubRepository.findAllByDeletedAtIsNull();

        List<HubResponse> resHub = hub.stream()
                .map(HubResponse::new)
                .collect(Collectors.toCollection(ArrayList::new));

        return resHub;
    }


    // 허브 생성
    @CacheEvict(cacheNames = "hubList", allEntries = true)
    @Transactional
    public HubResponse createHub(HubCreateRequest hubRequest){
        Hub hub = new Hub(hubRequest.getName()
                ,hubRequest.getAddress()
                ,hubRequest.getLatitude(),
                hubRequest.getLongitude(),
                hubRequest.getHubType());

        Hub savedHub = hubRepository.save(hub);

        return new HubResponse(savedHub);
    }

    // 허브 수정
    @Caching(evict = {
            @CacheEvict(cacheNames = "hubs", key = "#hubId"),
            @CacheEvict(cacheNames = "hubList", allEntries = true),
            @CacheEvict(
                    cacheNames = {
                            "hubRoutes",
                            "hubRouteList",
                            "directRoutes",
                            "routePaths"
                    },
                    allEntries = true
            )
    })
    @Transactional
    public void updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        hub.update(request);
    }


    @Caching(evict = {
            @CacheEvict(cacheNames = "hubs", key = "#hubId"),
            @CacheEvict(cacheNames = "hubList", allEntries = true),
            @CacheEvict(
                    cacheNames = {
                            "hubRoutes",
                            "hubRouteList",
                            "directRoutes",
                            "routePaths"
                    },
                    allEntries = true
            )
    })

    @Transactional
    public void requestDeactivation(UUID hubId) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        if(hub.getStatus() != HubStatus.ACTIVE){
            throw new BusinessException(HubErrorCode.HUB_INACTIVE);
        }

        hub.softDeactivation();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "hubs", key = "#hubId"),
            @CacheEvict(cacheNames = "hubList", allEntries = true),
            @CacheEvict(
                    cacheNames = {
                            "hubRoutes",
                            "hubRouteList",
                            "directRoutes",
                            "routePaths"
                    },
                    allEntries = true
            )
    })
    @Transactional
    public void completeDeactivation(UUID hubId, UUID deletedBy) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() ->
                        new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        if (hub.getStatus() != HubStatus.DEACTIVATING) {
            throw new BusinessException(HubErrorCode.HUB_INACTIVE);
        }

        List<HubRoute> routes =
                hubRouteRepository.findAllActiveRoutesByHubId(hubId);

        routes.forEach(route -> route.softDelete(deletedBy));
        hub.softDelete(deletedBy);
    }












}
