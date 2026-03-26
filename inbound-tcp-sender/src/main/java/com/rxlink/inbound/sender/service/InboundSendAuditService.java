package com.rxlink.inbound.sender.service;

import com.rxlink.inbound.sender.mongo.InboundSendAudit;
import com.rxlink.inbound.sender.mongo.InboundSendAuditRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class InboundSendAuditService {

    private final InboundSendAuditRepository auditRepository;
    private final String serviceName;
    private final int rxclaimPort;

    public InboundSendAuditService(
            InboundSendAuditRepository auditRepository,
            @org.springframework.beans.factory.annotation.Value("${inbound.sender.service-name:rxlink-inbound-sender-4111}") String serviceName,
            @org.springframework.beans.factory.annotation.Value("${inbound.sender.tcp.remote-port:4111}") int rxclaimPort) {
        this.auditRepository = auditRepository;
        this.serviceName = serviceName;
        this.rxclaimPort = rxclaimPort;
    }

    @Async
    public void recordSent(
            String correlationId,
            String routingKey,
            int requestBytes) {
        InboundSendAudit doc = new InboundSendAudit();
        doc.setId(UUID.randomUUID().toString());
        doc.setCorrelationId(correlationId);
        doc.setRoutingKey(routingKey);
        doc.setStatus("SENT");
        doc.setEventType("REQUEST_SENT");
        doc.setServiceName(serviceName);
        doc.setRxclaimPort(rxclaimPort);
        doc.setRequestBytes(requestBytes);
        doc.setResponseBytes(0);
        doc.setSocketWaitMs(0);
        doc.setCreatedAt(Instant.now());
        auditRepository.save(doc);
    }

    @Async
    public void recordResponse(
            String correlationId,
            String routingKey,
            String status,
            String detail,
            int responseBytes,
            long socketWaitMs) {
        InboundSendAudit doc = new InboundSendAudit();
        doc.setId(UUID.randomUUID().toString());
        doc.setCorrelationId(correlationId);
        doc.setRoutingKey(routingKey);
        doc.setStatus(status);
        doc.setEventType("RESPONSE_RECEIVED");
        doc.setServiceName(serviceName);
        doc.setRxclaimPort(rxclaimPort);
        doc.setRequestBytes(0);
        doc.setResponseBytes(responseBytes);
        doc.setSocketWaitMs(socketWaitMs);
        doc.setDetail(detail);
        doc.setCreatedAt(Instant.now());
        auditRepository.save(doc);
    }
}
