package com.sparta.company.client.hub;

import com.sparta.common.exception.BusinessException;
import com.sparta.common.exception.ErrorCode;
import com.sparta.company.client.hub.HubClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.sparta.company.config.RedisCacheConfig.HUB_EXISTS_CACHE;

/**
 * HubClient 호출 결과(존재 여부)를 Redis에 캐싱.
 *
 * FeignClient 인터페이스에는 @Cacheable을 직접 못 붙이므로(구현체가 Spring이 만드는
 * 프록시라 AOP 캐시 프록시가 한 번 더 감싸기 애매함), 이렇게 얇은 서비스로 감싸서 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HubQueryService {

    private final HubClient hubClient;

    @Cacheable(cacheNames = HUB_EXISTS_CACHE, key = "#hubId")
    public boolean existsHub(UUID hubId) {
        try {
            var response = hubClient.getHub(hubId);
            // Hub 서비스가 200을 주면서도 success=false/data=null을 내려주는 경우를 대비
            return response != null && response.isSuccess() && response.getData() != null;
        } catch (FeignException.NotFound e) {
            return false;
        } catch (FeignException e) {
            // 404가 아닌 그 외 실패(타임아웃, 5xx 등)는 "허브가 없다"와 의미가 다르므로
            // false로 뭉개지 않고 별도 예외로 알린다.
            log.error("Hub 서비스 호출 실패 (hubId={})", hubId, e);
            throw new BusinessException(ErrorCode.FEIGN_CLIENT_ERROR);
        }
    }

    /**
     * 향후 Hub 서비스가 "허브 삭제/변경" 이벤트를 Kafka 등으로 발행하게 되면,
     * 그 이벤트를 구독하는 리스너에서 이 메서드를 호출해 TTL을 기다리지 않고
     * 즉시 캐시를 무효화할 수 있다. 지금은 어디서도 호출하지 않는 확장 포인트.
     */
    @CacheEvict(cacheNames = HUB_EXISTS_CACHE, key = "#hubId")
    public void evict(UUID hubId) {
    }
}
