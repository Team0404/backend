package com.sparta.gateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistChecker {

    private static final String KEY_PREFIX = "access-token:blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> isBlacklisted(String tokenId) {
        return redisTemplate.hasKey(KEY_PREFIX + tokenId);
    }
}
