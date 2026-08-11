package com.sparta.company.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * 캐시별로 TTL을 다르게 가져간다.
 *
 * - hub-exists : 허브 존재 여부(Boolean). 허브 정보는 거의 안 바뀌므로 TTL을 길게(10분).
 * - user-info  : 사용자 상세정보(UserInfoResponse, hubId/companyId 포함). 역할/소속이
 *                바뀔 수 있는 값이라 상대적으로 짧게(2분).
 *
 * TTL만으로 무효화하는 구조라, 그 사이에 Hub/User 쪽 데이터가 바뀌면 최대 TTL만큼
 * 오래된 값을 볼 수 있다. 실시간 반영이 필요해지면 Hub/User 서비스가 변경 이벤트를
 * Kafka 등으로 발행하고, 이 서비스가 그걸 구독해서 HubCacheEvictor/UserCacheEvictor를
 * 호출해 즉시 지우는 방식으로 확장할 수 있다 (현재는 TTL 방식만 적용).
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String HUB_EXISTS_CACHE = "hub-exists";
    public static final String USER_INFO_CACHE = "user-info";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                HUB_EXISTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)),
                USER_INFO_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(2))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
