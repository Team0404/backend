package com.sparta.user.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenStoreTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        refreshTokenStore = new RefreshTokenStore(redisTemplate);
    }

    @Test
    void savesRefreshTokenWithTtl() {
        UUID userId = UUID.randomUUID();
        Duration ttl = Duration.ofDays(14);

        refreshTokenStore.save(userId, "refresh-token", ttl);

        verify(valueOperations).set("refresh-token:" + userId, "refresh-token", ttl);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rotateReturnsTrueOnlyWhenStoredTokenMatches() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("refresh-token:" + userId)),
                eq("old-token"),
                eq("new-token"),
                eq(String.valueOf(Duration.ofDays(14).toMillis()))
        )).thenReturn(1L);

        boolean rotated = refreshTokenStore.rotate(
                userId, "old-token", "new-token", Duration.ofDays(14));

        assertThat(rotated).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void revokeReturnsFalseWhenStoredTokenDoesNotMatch() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("wrong-token")))
                .thenReturn(0L);

        assertThat(refreshTokenStore.revoke(UUID.randomUUID(), "wrong-token")).isFalse();
    }
}
