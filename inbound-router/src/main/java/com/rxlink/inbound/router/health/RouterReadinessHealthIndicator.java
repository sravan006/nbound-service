package com.rxlink.inbound.router.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("routerReadiness")
public class RouterReadinessHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redis;
    private final boolean checkSender;
    private final String senderHealthUrl;

    public RouterReadinessHealthIndicator(
            StringRedisTemplate redis,
            @Value("${inbound.router.readiness.check-sender-enabled:false}") boolean checkSender,
            @Value("${inbound.router.readiness.sender-health-url:http://localhost:8083/actuator/health}") String senderHealthUrl) {
        this.redis = redis;
        this.checkSender = checkSender;
        this.senderHealthUrl = senderHealthUrl;
    }

    @Override
    public Health health() {
        try (var connection = redis.getConnectionFactory().getConnection()) {
            connection.ping();
        } catch (Exception e) {
            return Health.down(e).withDetail("redis", "unreachable").build();
        }
        if (checkSender) {
            try {
                SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
                rf.setConnectTimeout(1000);
                rf.setReadTimeout(1000);
                RestClient.builder().requestFactory(rf).build().get().uri(senderHealthUrl).retrieve().toBodilessEntity();
            } catch (Exception e) {
                return Health.down(e).withDetail("sender", "unreachable").build();
            }
        }
        return Health.up().build();
    }
}
