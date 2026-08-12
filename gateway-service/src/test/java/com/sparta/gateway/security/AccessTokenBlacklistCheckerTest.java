package com.sparta.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessTokenBlacklistCheckerTest {

    @Test
    void checksBlacklistByAccessTokenId() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        when(redisTemplate.hasKey("access-token:blacklist:access-token-id"))
                .thenReturn(Mono.just(true));
        AccessTokenBlacklistChecker checker = new AccessTokenBlacklistChecker(redisTemplate);

        StepVerifier.create(checker.isBlacklisted("access-token-id"))
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).hasKey("access-token:blacklist:access-token-id");
    }
}
