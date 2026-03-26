package com.rxlink.inbound.sender.service;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.SendResponseDto;
import com.rxlink.inbound.sender.orchestration.OrchestrationApplier;
import com.rxlink.inbound.sender.orchestration.OrchestrationRule;
import com.rxlink.inbound.sender.orchestration.OrchestrationRuleService;
import com.rxlink.inbound.sender.tcp.TcpDispatchService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
public class SendPipelineService {

    private static final Logger log = LoggerFactory.getLogger(SendPipelineService.class);

    private final OrchestrationRuleService orchestrationRuleService;
    private final OrchestrationApplier orchestrationApplier;
    private final TcpDispatchService tcpDispatchService;
    private final InboundSendAuditService inboundSendAuditService;
    private final Counter sent;
    private final Counter sendFailed;
    private final Timer rxclaimLatency;

    public SendPipelineService(
            OrchestrationRuleService orchestrationRuleService,
            OrchestrationApplier orchestrationApplier,
            TcpDispatchService tcpDispatchService,
            InboundSendAuditService inboundSendAuditService,
            MeterRegistry meterRegistry) {
        this.orchestrationRuleService = orchestrationRuleService;
        this.orchestrationApplier = orchestrationApplier;
        this.tcpDispatchService = tcpDispatchService;
        this.inboundSendAuditService = inboundSendAuditService;
        this.sent = Counter.builder("sender_messages_processed_total").register(meterRegistry);
        this.sendFailed = Counter.builder("sender_messages_failed_total").register(meterRegistry);
        this.rxclaimLatency = Timer.builder("sender_rxclaim_latency_seconds").register(meterRegistry);
    }

    public SendResponseDto handle(InboundMessageDto dto) {
        String correlationId = dto.correlationId() != null && !dto.correlationId().isBlank()
                ? dto.correlationId()
                : UUID.randomUUID().toString();
        try {
            byte[] raw = Base64.getDecoder().decode(dto.payloadBase64());
            OrchestrationRule rule = orchestrationRuleService.resolve();
            byte[] transformed = orchestrationApplier.apply(rule, raw);
            inboundSendAuditService.recordSent(correlationId, dto.routingKey(), transformed.length);
            long startNs = System.nanoTime();
            byte[] response = tcpDispatchService.sendAndReceive(transformed);
            long waitNs = System.nanoTime() - startNs;
            rxclaimLatency.record(waitNs, java.util.concurrent.TimeUnit.NANOSECONDS);
            String responseB64 = Base64.getEncoder().encodeToString(response);
            sent.increment();
            inboundSendAuditService.recordResponse(correlationId, dto.routingKey(), "SENT", null, response.length, waitNs / 1_000_000);
            return SendResponseDto.accepted(correlationId, responseB64);
        } catch (IllegalArgumentException e) {
            sendFailed.increment();
            inboundSendAuditService.recordResponse(correlationId, dto.routingKey(), "BAD_PAYLOAD", e.getMessage(), 0, 0);
            return SendResponseDto.error(correlationId, "Invalid base64 payload");
        } catch (Exception e) {
            log.warn("TCP send failed correlationId={}", correlationId, e);
            sendFailed.increment();
            inboundSendAuditService.recordResponse(correlationId, dto.routingKey(), "TCP_ERROR", e.getMessage(), 0, 0);
            return SendResponseDto.error(correlationId, e.getMessage());
        }
    }
}
