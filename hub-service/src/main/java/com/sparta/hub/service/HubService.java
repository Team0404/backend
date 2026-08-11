package com.sparta.hub.service;

import com.sparta.common.exception.BusinessException;
import com.sparta.hub.dto.request.HubCreateRequest;
import com.sparta.hub.dto.request.HubUpdateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.exception.HubErrorCode;
import com.sparta.hub.repository.HubRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubService {
    private final HubRepository hubRepository;


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
    @Transactional(readOnly = true)
    public List<HubResponse> findAllHubs(){
        List<Hub> hub = hubRepository.findAllByDeletedAtIsNull();

        List<HubResponse> resHub = hub.stream()
                .map(HubResponse::new)
                .toList();

        return resHub;
    }


    // 허브 생성
    @Transactional
    public HubResponse createHub(HubCreateRequest hubRequest){
        Hub hub = new Hub(hubRequest.getName()
                ,hubRequest.getAddress()
                ,hubRequest.getLatitude(),
                hubRequest.getLongitude());

        Hub savedHub = hubRepository.save(hub);

        return new HubResponse(savedHub);
    }

    // 허브 수정
    @Transactional
    public void updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        hub.update(request);
    }

    // 허브 삭제(SoftDelete)
    @Transactional
    public void deleteHub(UUID hubId){
        Hub hub = hubRepository.findByHubIdAndDeletedAtIsNull(hubId)
                .orElseThrow(() -> new BusinessException(HubErrorCode.HUB_NOT_FOUND));

        hub.softDelete(hubId);


    }

}
