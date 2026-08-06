package com.sparta.company.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * AuthHeaders(X-User-Id/Username/Role)에는 hubId/companyId가 없기 때문에,
 * HUB_MANAGER/SUPPLIER_MANAGER의 스코프(담당 허브, 소속 업체) 판단을 위해
 * User 서비스에 직접 조회한다.
 *
 * TODO: Gateway가 JWT의 hubId/companyId를 X-Hub-Id / X-Company-Id 헤더로 내려주도록
 *       바뀌면 이 FeignClient 호출은 제거하고 헤더에서 바로 읽으면 된다.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable("userId") String userId);
}
