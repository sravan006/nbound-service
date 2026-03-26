package com.rxlink.inbound.router.service;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.RouteResponseDto;
import com.rxlink.inbound.common.api.SendResponseDto;
import com.rxlink.inbound.router.client.SenderForwardClient;
import com.rxlink.inbound.router.routing.RoutingKeyResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final RoutingKeyResolver routingKeyResolver;
    private final RouteRuleService routeRuleService;
    private final SenderForwardClient senderForwardClient;
    private final InboundAuditService inboundAuditService;
    private final TaskExecutor routerTaskExecutor;
    private final Optional<String> defaultTargetUrl;
    private final Timer routeTimer;
    private final Counter routeSuccess;
    private final Counter routeFailure;

    public RoutingService(
            RoutingKeyResolver routingKeyResolver,
            RouteRuleService routeRuleService,
            SenderForwardClient senderForwardClient,
            InboundAuditService inboundAuditService,
            @Qualifier("routerTaskExecutor") TaskExecutor routerTaskExecutor,
            MeterRegistry meterRegistry,
            @Value("${inbound.router.default-target-url:}") String defaultTargetUrl) {
        this.routingKeyResolver = routingKeyResolver;
        this.routeRuleService = routeRuleService;
        this.senderForwardClient = senderForwardClient;
        this.inboundAuditService = inboundAuditService;
        this.routerTaskExecutor = routerTaskExecutor;
        this.defaultTargetUrl = Optional.ofNullable(defaultTargetUrl).filter(s -> !s.isBlank());
        this.routeTimer = Timer.builder("router_message_latency_seconds").register(meterRegistry);
        this.routeSuccess = Counter.builder("router_messages_routed_total").register(meterRegistry);
        this.routeFailure = Counter.builder("router_messages_failed_total").register(meterRegistry);
    }

    public RouteResponseDto route(InboundMessageDto dto) {
        return routeTimer.record(() -> CompletableFuture.supplyAsync(() -> doRoute(dto), routerTaskExecutor).join());
    }

    private RouteResponseDto doRoute(InboundMessageDto dto) {
        String correlationId = dto.correlationId() != null && !dto.correlationId().isBlank()
                ? dto.correlationId()
                : UUID.randomUUID().toString();
        try {
            byte[] payload = Base64.getDecoder().decode(dto.payloadBase64());
            String routingKey = routingKeyResolver.resolve(payload, dto.routingKey());
            Optional<String> target = routeRuleService.resolveTargetUrl(routingKey);
            String baseUrl = target.or(() -> defaultTargetUrl)
                    .orElse(null);
            if (baseUrl == null) {
                routeFailure.increment();
                inboundAuditService.recordRoute(correlationId, routingKey, null, "NO_TARGET",
                        "No Redis rule and no default-target-url");
                return RouteResponseDto.error(correlationId, "No route target for key: " + routingKey);
            }
            InboundMessageDto forward = new InboundMessageDto(
                    dto.payloadBase64(),
                    correlationId,
                    routingKey,
                    dto.sourceChannel(),
                    dto.attributes());
            SendResponseDto senderResponse = senderForwardClient.forward(baseUrl, forward);
            if (!"SENT".equals(senderResponse.status())) {
                routeFailure.increment();
                inboundAuditService.recordRoute(correlationId, routingKey, baseUrl, "DOWNSTREAM_ERROR", senderResponse.detail());
                return RouteResponseDto.error(correlationId, senderResponse.detail());
            }
            routeSuccess.increment();
            inboundAuditService.recordRoute(correlationId, routingKey, baseUrl, "FORWARDED", null);
            return RouteResponseDto.ok(correlationId, routingKey, baseUrl, senderResponse.responsePayloadBase64());
        } catch (IllegalArgumentException e) {
            routeFailure.increment();
            inboundAuditService.recordRoute(correlationId, null, null, "BAD_PAYLOAD", e.getMessage());
            return RouteResponseDto.error(correlationId, "Invalid base64 payload");
        } catch (Exception e) {
            log.warn("Route failed correlationId={}", correlationId, e);
            routeFailure.increment();
            inboundAuditService.recordRoute(correlationId, null, null, "DOWNSTREAM_ERROR", e.getMessage());
            return RouteResponseDto.error(correlationId, e.getMessage());
        }
    }
}
