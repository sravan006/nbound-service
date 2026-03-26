package com.rxlink.inbound.receiver.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ReceiverConfigService {

    private static final String CONFIG_PREFIX = "inbound_receiver:config:";

    private final StringRedisTemplate redis;
    private final String serviceName;
    private final Cache<String, String> l1;
    private final String defaultRouterBaseUrl;
    private final int defaultRouterConnectTimeoutMs;
    private final int defaultRouterReadTimeoutMs;
    private final String defaultRouterPath;
    private final int defaultMaxConnections;

    public ReceiverConfigService(
            StringRedisTemplate redis,
            @Value("${inbound.receiver.service-name:rxlink-inbound-receiver-7001}") String serviceName,
            @Value("${inbound.receiver.router.base-url:http://localhost:8080}") String defaultRouterBaseUrl,
            @Value("${inbound.receiver.router.connect-timeout-ms:5000}") int defaultRouterConnectTimeoutMs,
            @Value("${inbound.receiver.router.read-timeout-ms:120000}") int defaultRouterReadTimeoutMs,
            @Value("${inbound.receiver.router.route-path:/api/v1/route}") String defaultRouterPath,
            @Value("${inbound.receiver.tcp.max-connections:500}") int defaultMaxConnections,
            @Value("${inbound.receiver.cache.ttl-seconds:300}") long l1TtlSeconds) {
        this.redis = redis;
        this.serviceName = serviceName;
        this.defaultRouterBaseUrl = defaultRouterBaseUrl;
        this.defaultRouterConnectTimeoutMs = defaultRouterConnectTimeoutMs;
        this.defaultRouterReadTimeoutMs = defaultRouterReadTimeoutMs;
        this.defaultRouterPath = defaultRouterPath;
        this.defaultMaxConnections = defaultMaxConnections;
        this.l1 = Caffeine.newBuilder()
                .maximumSize(256)
                .expireAfterWrite(Duration.ofSeconds(l1TtlSeconds))
                .build();
    }

    public String routerBaseUrl() {
        return get("routerBaseUrl", defaultRouterBaseUrl);
    }

    public int routerConnectTimeoutMs() {
        return parseInt(get("routerConnectTimeoutMs", Integer.toString(defaultRouterConnectTimeoutMs)), defaultRouterConnectTimeoutMs);
    }

    public int routerReadTimeoutMs() {
        return parseInt(get("routerReadTimeoutMs", Integer.toString(defaultRouterReadTimeoutMs)), defaultRouterReadTimeoutMs);
    }

    public String routePath() {
        return get("routerRoutePath", defaultRouterPath);
    }

    public int maxConnections() {
        return parseInt(get("maxConnections", Integer.toString(defaultMaxConnections)), defaultMaxConnections);
    }

    public void clearLocalCache() {
        l1.invalidateAll();
    }

    private String get(String field, String fallback) {
        String key = serviceName + ":" + field;
        String cached = l1.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        Object redisValue = redis.opsForHash().get(CONFIG_PREFIX + serviceName, field);
        String value = redisValue == null ? fallback : redisValue.toString();
        l1.put(key, value);
        return value;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
