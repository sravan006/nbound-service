package com.rxlink.inbound.sender.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OrchestrationRuleService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationRuleService.class);
    private static final String REDIS_PREFIX = "rxlink:sender:orchestrate:";

    private final StringRedisTemplate redis;
    private final String instanceId;
    private final ObjectMapper objectMapper;
    private final LoadingCache<String, OrchestrationRule> localCache;

    public OrchestrationRuleService(
            StringRedisTemplate redis,
            @Value("${inbound.sender.instance-id:default}") String instanceId,
            ObjectMapper objectMapper,
            @Value("${inbound.sender.orchestration.cache-ttl-seconds:300}") long cacheTtlSeconds) {
        this.redis = redis;
        this.instanceId = instanceId;
        this.objectMapper = objectMapper;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .maximumSize(500)
                .build(this::loadFromRedis);
    }

    private OrchestrationRule loadFromRedis(String cacheKey) {
        String raw = redis.opsForValue().get(REDIS_PREFIX + cacheKey);
        if (raw == null || raw.isBlank()) {
            return new OrchestrationRule();
        }
        try {
            return objectMapper.readValue(raw, OrchestrationRule.class);
        } catch (Exception e) {
            log.warn("Bad orchestration JSON in Redis for key={}", cacheKey, e);
            return new OrchestrationRule();
        }
    }

    public OrchestrationRule resolve() {
        return localCache.get(instanceId);
    }

    public void clearLocalCache() {
        localCache.invalidateAll();
    }
}
