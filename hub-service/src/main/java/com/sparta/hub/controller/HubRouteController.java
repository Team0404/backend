package com.sparta.hub.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.hub.dto.request.HubRouteCreateRequest;
import com.sparta.hub.dto.request.HubRouteUpdateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.dto.response.HubRoutePathResponse;
import com.sparta.hub.dto.response.HubRouteResponse;
import com.sparta.hub.service.HubRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "HubRoute",
        description = "허브 간 이동정보 생성,조회,수정,삭제 API"
)
@RestController
@RequestMapping("/api/v1/hub-routes")
@RequiredArgsConstructor
public class HubRouteController {
    private final HubRouteService hubRouteService;



    // 이동 정보 생성
    @Operation(
            summary = "허브 이동 정보 생성",
            description = "새로운 허브 이동 정보를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "허브 이동 정보 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 값"
            )
    })
    @PostMapping()
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<HubRouteResponse>> createHubRouter(
            @Valid @RequestBody HubRouteCreateRequest request
    ){

        HubRouteResponse response = hubRouteService.createHubRoute(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("허브 이동 경로 생성 완료",response));

    }

    // 이동 정보 단건 조회
    @Operation(
            summary = "허브 이동정보 단건 조회 ",
            description = "허브 간 이동 정보 단건을 조회 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "허브 이동정보 단건 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 값"
            )
    })

    @GetMapping("/{routeId}")
    public ApiResponse<HubRouteResponse> findHubRoute(@PathVariable UUID routeId){

        return ApiResponse.success(hubRouteService.findHubRoute(routeId));
    }


    // 이동 정보 목록,검색
    @Operation(
            summary = "허브간 이동 정보 전체 조회",
            description = "허브와 허브가 연결 되어 있는 이동 정보를 조회 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "허브 이동정보 목록, 검색 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 값"
            )
    })
    @GetMapping()
    public ApiResponse<List<HubRouteResponse>> findAllHubRoute(){
        return ApiResponse.success(hubRouteService.findAllHubRoutes());
    }

    // 출발,도착 경로 조회
    @GetMapping("/path")
    public ApiResponse<HubRoutePathResponse> hubRoutePath(@RequestParam UUID dhId, @RequestParam UUID arId){
        HubRoutePathResponse response = hubRouteService.findPath(dhId,arId);

        return ApiResponse.success("출발,도착 경로 조회 성공", response);
    }

    // 이동 정보 수정
    @PatchMapping("/{routeId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<Void> updateHubRoute(@PathVariable UUID routeId,
                                            @Valid @RequestBody HubRouteUpdateRequest request){
        hubRouteService.hubRouteUpdate(routeId, request);
        return ApiResponse.success("허브 라우터 수정 완료", null);
    }


    //이동 정보 삭제
    @DeleteMapping("/{routeId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<Void> deleteHubRoute(@PathVariable UUID routeId,
                                            @CurrentUser UserPrincipal principal){
        hubRouteService.deleteHubRoute(routeId, principal.getUserId());

        return ApiResponse.success("허브 경로 삭제 완료", null);

    }





}
