package com.sparta.hub.controller;

import com.sparta.common.response.ApiResponse;
import com.sparta.common.security.CurrentUser;
import com.sparta.common.security.UserPrincipal;
import com.sparta.hub.dto.request.HubCreateRequest;
import com.sparta.hub.dto.request.HubUpdateRequest;
import com.sparta.hub.dto.response.HubResponse;
import com.sparta.hub.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Hub",
        description = "허브 생성, 조회, 수정, 삭제 API"
)
@RestController
@RequestMapping("/api/v1/hubs")
@RequiredArgsConstructor
public class HubController {
    private final HubService hubService;

    // 전체 허브 조회
    @Operation(summary = "허브 전체 조회",
            description = "삭제되지 않은 모든 허브를 조회합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "허브 전체 조회 성공")})
    @GetMapping()
    public ApiResponse<List<HubResponse>> findAllHubs() {
        return ApiResponse.success(
                                "허브 조회 성공",
                                hubService.findAllHubs());

    }

    // 허브 단건 조회
    @Operation(
            summary = "허브 단건 조회",
            description = "허브 ID로 삭제되지 않은 허브를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "허브 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "허브를 찾을 수 없음"
            )
    })
    @GetMapping("/{hubId}")
    public ApiResponse<HubResponse> findHub(@PathVariable UUID hubId) {


        return  ApiResponse.success("허브 조회 성공", hubService.findHub(hubId));
    }

    // 허브 생성
    @Operation(
            summary = "허브 생성",
            description = "새로운 허브를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "허브 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 값"
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<ApiResponse<HubResponse>> createHub(
            @Valid @RequestBody HubCreateRequest request) {
       HubResponse response = hubService.createHub(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success("허브 생성 완료", response));

    }

    @Operation(
            summary = "허브 수정",
            description = "허브의 이름, 주소, 위도 및 경도를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "허브 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "허브를 찾을 수 없음"
            )
    })
    @PatchMapping("/{hubId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<Void> updateHub(@PathVariable UUID hubId,
                                       @Valid @RequestBody HubUpdateRequest request) {

        hubService.updateHub(hubId, request);

        return ApiResponse.success("허브 수정 완료", null);
    }

    @Operation(
            summary = "허브 삭제",
            description = "허브를 Soft Delete 처리합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "허브 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "허브를 찾을 수 없음"
            )
    })
    @DeleteMapping("/{hubId}")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<Void> requestDeactivation(@PathVariable UUID hubId,
                                                 @CurrentUser UserPrincipal userPrincipal) {
        hubService.requestDeactivation(hubId);

        return ApiResponse.success("허브가 비활성화 대기 상태로 변경되었습니다.", null);

    }

    @PatchMapping("/{hubId}/deactivation/complete")
    @PreAuthorize("hasRole('MASTER')")
    public ApiResponse<Void> completeDeactivation(
            @PathVariable UUID hubId,
            @CurrentUser UserPrincipal principal
    ) {
        hubService.completeDeactivation(
                hubId,
                principal.getUserId()
        );

        return ApiResponse.success("허브 최종 삭제 완료", null);
    }


}
