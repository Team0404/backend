package com.sparta.hub.config;

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

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory){

        GenericJackson2JsonRedisSerializer valueSerializer =
                GenericJackson2JsonRedisSerializer.builder()
                        .defaultTyping(true)
                        .typeHintPropertyName("@class")
                        .build();


        RedisCacheConfiguration configuration =
                RedisCacheConfiguration.defaultCacheConfig()
                        // 10분 후 자동 삭제
                        .entryTtl(Duration.ofMinutes(10))

                        // null 조회 저장 x
                        .disableCachingNullValues()

                        // Redis Key 문자열로 저장
                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                        )

                        // Redis Value JSON으로 저장
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(valueSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .transactionAware()
                .build();

    }
}
