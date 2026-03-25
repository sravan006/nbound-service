package com.rxlink.inbound.sender.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundSendAuditRepository extends MongoRepository<InboundSendAudit, String> {
}
