package com.rxlink.inbound.router.service;

import com.rxlink.inbound.router.config.CacheConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RouteRuleService {

    private static final Logger log = LoggerFactory.getLogger(RouteRuleService.class);
    private static final String REDIS_KEY_PREFIX = "inbound_routing:rules:";

    private final StringRedisTemplate redis;
    private final Cache routeRuleCache;
    private final Counter cacheHit;
    private final Counter cacheMiss;

    public RouteRuleService(
            StringRedisTemplate redis,
            CacheManager cacheManager,
            MeterRegistry meterRegistry) {
        this.redis = redis;
        Cache cache = cacheManager.getCache(CacheConfig.ROUTE_RULES);
        if (cache == null) {
            throw new IllegalStateException("Missing cache: " + CacheConfig.ROUTE_RULES);
        }
        this.routeRuleCache = cache;
        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineNative) {
            CaffeineCacheMetrics.monitor(meterRegistry, caffeineNative, CacheConfig.ROUTE_RULES);
        }
        this.cacheHit = Counter.builder("router_routing_cache_hits_total").register(meterRegistry);
        this.cacheMiss = Counter.builder("router_routing_cache_misses_total").register(meterRegistry);
    }

    public Optional<String> resolveTargetUrl(String routingKey) {
        Cache.ValueWrapper cached = routeRuleCache.get(routingKey);
        if (cached != null && cached.get() instanceof String s) {
            cacheHit.increment();
            return Optional.of(s);
        }
        cacheMiss.increment();
        String fromRedis = redis.opsForValue().get(REDIS_KEY_PREFIX + routingKey);
        if (fromRedis != null) {
            routeRuleCache.put(routingKey, fromRedis);
            return Optional.of(fromRedis);
        }
        log.debug("No route rule for key={}", routingKey);
        return Optional.empty();
    }

    public void evictLocal(String routingKey) {
        routeRuleCache.evict(routingKey);
    }

    public void evictAllLocalRouteCache() {
        routeRuleCache.clear();
    }
}
