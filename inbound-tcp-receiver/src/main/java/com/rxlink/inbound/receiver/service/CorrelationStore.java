package com.rxlink.inbound.receiver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CorrelationStore {

    private static final String KEY_PREFIX = "rxlink:corr:receiver:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public CorrelationStore(
            StringRedisTemplate redis,
            @Value("${inbound.receiver.correlation.ttl-seconds:86400}") long ttlSeconds) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public void markReceived(String correlationId, int byteLength) {
        redis.opsForValue().set(KEY_PREFIX + correlationId, "RECEIVED:" + byteLength, ttl);
    }

    public void markForwarded(String correlationId) {
        redis.opsForValue().set(KEY_PREFIX + correlationId, "FORWARDED", ttl);
    }

    public void markFailed(String correlationId, String reason) {
        String v = "FAILED:" + (reason == null ? "" : reason.substring(0, Math.min(reason.length(), 200)));
        redis.opsForValue().set(KEY_PREFIX + correlationId, v, ttl);
    }
}
