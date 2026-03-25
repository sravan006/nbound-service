package com.rxlink.inbound.router.service;

import com.rxlink.inbound.router.mongo.InboundRouteAudit;
import com.rxlink.inbound.router.mongo.InboundRouteAuditRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class InboundAuditService {

    private final InboundRouteAuditRepository auditRepository;

    public InboundAuditService(InboundRouteAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Async
    public void recordRoute(
            String correlationId,
            String routingKey,
            String targetSenderUrl,
            String status,
            String detail) {
        InboundRouteAudit doc = new InboundRouteAudit();
        doc.setId(UUID.randomUUID().toString());
        doc.setCorrelationId(correlationId);
        doc.setRoutingKey(routingKey);
        doc.setTargetSenderUrl(targetSenderUrl);
        doc.setStatus(status);
        doc.setDetail(detail);
        doc.setCreatedAt(Instant.now());
        auditRepository.save(doc);
    }
}
