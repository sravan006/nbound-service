package com.rxlink.inbound.router.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ROUTE_RULES = "routeRules";

    @Bean
    public CacheManager caffeineCacheManager(
            @Value("${inbound.router.cache.l1.maximum-size:10000}") long maximumSize,
            @Value("${inbound.router.cache.l1.ttl-seconds:300}") long ttlSeconds) {
        CaffeineCacheManager manager = new CaffeineCacheManager(ROUTE_RULES);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .recordStats());
        return manager;
    }
}
