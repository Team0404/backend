package com.sparta.user.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Repository
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh-token:";
    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "access-token:blacklist:";
    private static final String BLACKLIST_VALUE = "logout";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> LOGOUT_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end

            local redisTime = redis.call('TIME')
            local nowMillis = redisTime[1] * 1000 + math.floor(redisTime[2] / 1000)
            local ttlMillis = tonumber(ARGV[3]) - nowMillis

            if ttlMillis > 0 then
                redis.call('SET', KEYS[2], ARGV[2], 'PX', ttlMillis)
            end

            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(UUID userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    public boolean rotate(UUID userId, String currentToken, String newToken, Duration ttl) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(userId)),
                currentToken,
                newToken,
                String.valueOf(ttl.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    public boolean revokeAndBlacklistAccessToken(UUID userId, String refreshToken,
                                                 String accessTokenId, long accessTokenExpiresAt) {
        Long result = redisTemplate.execute(
                LOGOUT_SCRIPT,
                List.of(key(userId), blacklistKey(accessTokenId)),
                refreshToken,
                BLACKLIST_VALUE,
                String.valueOf(accessTokenExpiresAt)
        );
        return Long.valueOf(1L).equals(result);
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }

    private String blacklistKey(String accessTokenId) {
        return ACCESS_TOKEN_BLACKLIST_KEY_PREFIX + accessTokenId;
    }
}
