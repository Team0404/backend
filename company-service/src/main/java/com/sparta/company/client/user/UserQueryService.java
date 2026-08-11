package com.sparta.company.client.user;

import com.sparta.common.dto.UserInfoResponse;
import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.common.response.ApiResponse;
import com.sparta.company.client.user.UserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.sparta.company.config.RedisCacheConfig.USER_INFO_CACHE;

/**
 * UserClient 호출 결과(사용자 상세정보)를 Redis에 캐싱.
 * HUB_MANAGER/SUPPLIER_MANAGER의 담당 허브·소속 업체를 알아내려고
 * 매 요청마다 User 서비스를 호출하던 부분을 캐싱으로 대체한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserClient userClient;

    @Cacheable(cacheNames = USER_INFO_CACHE, key = "#userId")
    public UserInfoResponse getUserInfo(UUID userId) {
        ApiResponse<UserInfoResponse> response;
        try {
            response = userClient.getUser(userId);
        } catch (FeignException e) {
            log.error("User 서비스 호출 실패 (userId={})", userId, e);
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR);
        }

        if (response == null || !response.isSuccess() || response.getData() == null) {
            log.warn("User 서비스가 유효하지 않은 사용자 정보를 반환함 (userId={})", userId);
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR);
        }

        return response.getData();
    }

    /**
     * 향후 User 서비스가 "역할/소속 변경" 이벤트를 Kafka 등으로 발행하게 되면,
     * 그 이벤트를 구독하는 리스너에서 이 메서드를 호출해 즉시 캐시를 무효화할 수 있다.
     * 지금은 어디서도 호출하지 않는 확장 포인트.
     */
    @CacheEvict(cacheNames = USER_INFO_CACHE, key = "#userId")
    public void evict(UUID userId) {
    }
}
