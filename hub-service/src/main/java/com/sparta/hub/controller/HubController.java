package com.sparta.hub.controller;

import com.sparta.hub.dto.request.HubCreateRequest;
import com.sparta.hub.dto.request.HubUpdateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.entity.Hub;
import com.sparta.hub.repository.HubRepository;
import com.sparta.hub.service.HubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/hubs")
@RequiredArgsConstructor
public class HubController {
    private final HubService hubService;
    private final HubRepository hubRepository;

    // 전체 허브 조회
    @GetMapping()
    public List<Hub> findAllHubs(){
        return hubRepository.findAll();
    }

    // 허브 단건 조회
    @GetMapping("/{id}")
    public HubResponse findHub(@PathVariable UUID id){

        return hubService.findHub(id);
    }

    // 허브 생성
    @PostMapping
    public ResponseEntity<String> createHub(@Valid @RequestBody HubCreateRequest request){
        hubService.createHub(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("허브 생성 완료");
    }

    @PatchMapping("/{hubId}")
    public ResponseEntity<String> updateHub(@PathVariable UUID hubId,
                            @Valid @RequestBody HubUpdateRequest request){
        hubService.updateHub(hubId,request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("허브 수정 완료");
    }

    @DeleteMapping("/{hubId}")
    public ResponseEntity<String> deleteHub(@PathVariable UUID hubId){
        hubService.deleteHub(hubId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body("허브 삭제 완료");
    }






}
