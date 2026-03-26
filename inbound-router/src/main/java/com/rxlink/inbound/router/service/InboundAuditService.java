package com.rxlink.inbound.router.service;

import com.rxlink.inbound.router.mongo.InboundRouteAudit;
import com.rxlink.inbound.router.mongo.InboundRouteAuditRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class InboundAuditService {

    private final InboundRouteAuditRepository auditRepository;
    private final boolean mongoAuditEnabled;

    public InboundAuditService(
            InboundRouteAuditRepository auditRepository,
            @Value("${inbound.router.audit.mongo-enabled:false}") boolean mongoAuditEnabled) {
        this.auditRepository = auditRepository;
        this.mongoAuditEnabled = mongoAuditEnabled;
    }

    @Async
    public void recordRoute(
            String correlationId,
            String routingKey,
            String targetSenderUrl,
            String status,
            String detail) {
        if (!mongoAuditEnabled) {
            return;
        }
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
