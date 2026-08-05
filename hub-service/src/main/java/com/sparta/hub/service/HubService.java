package com.sparta.hub.service;

import com.sparta.hub.dto.request.HubCreateRequest;
import com.sparta.hub.dto.request.HubUpdateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.repository.HubRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HubService {
    private final HubRepository hubRepository;


    public HubResponse findHub(UUID id){
        Hub hub = hubRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(()
                -> new RuntimeException("없는 번호 입니다."));

        return new HubResponse(hub);

    }

    @Transactional
    public void createHub(HubCreateRequest hubRequest){
        Hub hub = new Hub(hubRequest.getName()
                ,hubRequest.getAddress()
                ,hubRequest.getLatitude(),
                hubRequest.getLongitude());

        Hub savedHub = hubRepository.save(hub);
    }

    // 허브 수정
    @Transactional
    public void updateHub(UUID hubId, HubUpdateRequest request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new RuntimeException("없는 허브 번호 입니다"));

        hub.update(request);
    }

    @Transactional
    public void deleteHub(UUID hubId){
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new RuntimeException("없는 허브 번호 입니다"));

        hub.softDelete(hubId);


    }

}
