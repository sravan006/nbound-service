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

    public InboundSendAuditService(InboundSendAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Async
    public void record(String correlationId, String routingKey, String status, String detail) {
        InboundSendAudit doc = new InboundSendAudit();
        doc.setId(UUID.randomUUID().toString());
        doc.setCorrelationId(correlationId);
        doc.setRoutingKey(routingKey);
        doc.setStatus(status);
        doc.setDetail(detail);
        doc.setCreatedAt(Instant.now());
        auditRepository.save(doc);
    }
}
