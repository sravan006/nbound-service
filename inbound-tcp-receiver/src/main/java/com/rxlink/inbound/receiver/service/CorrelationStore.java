package com.rxlink.inbound.receiver.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CorrelationStore {

    private static final String KEY_PREFIX = "inbound_receiver:correlation:";

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final Cache<String, String> l1ConnectionCache;

    public CorrelationStore(
            StringRedisTemplate redis,
            @Value("${inbound.receiver.correlation.ttl-seconds:86400}") long ttlSeconds,
            @Value("${inbound.receiver.cache.ttl-seconds:300}") long cacheTtlSeconds) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
        this.l1ConnectionCache = Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .build();
    }

    public void markReceived(String scope, String correlationId, String connectionId, int byteLength) {
        String key = key(scope, correlationId);
        redis.opsForHash().put(key, "connectionId", connectionId);
        redis.opsForHash().put(key, "status", "RECEIVED");
        redis.opsForHash().put(key, "requestBytes", Integer.toString(byteLength));
        redis.expire(key, ttl);
        l1ConnectionCache.put(key, connectionId);
    }

    public void markForwarded(String scope, String correlationId) {
        String key = key(scope, correlationId);
        redis.opsForHash().put(key, "status", "FORWARDED");
        redis.expire(key, ttl);
    }

    public void markFailed(String scope, String correlationId, String reason) {
        String key = key(scope, correlationId);
        String detail = reason == null ? "" : reason.substring(0, Math.min(reason.length(), 200));
        redis.opsForHash().put(key, "status", "FAILED");
        redis.opsForHash().put(key, "detail", detail);
        redis.expire(key, ttl);
    }

    public Optional<String> getConnectionId(String scope, String correlationId) {
        String cacheKey = key(scope, correlationId);
        String cached = l1ConnectionCache.getIfPresent(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        Object fromRedis = redis.opsForHash().get(cacheKey, "connectionId");
        if (fromRedis == null) {
            return Optional.empty();
        }
        String value = fromRedis.toString();
        l1ConnectionCache.put(cacheKey, value);
        return Optional.of(value);
    }

    private static String key(String scope, String correlationId) {
        return KEY_PREFIX + scope + ":" + correlationId;
    }
}
