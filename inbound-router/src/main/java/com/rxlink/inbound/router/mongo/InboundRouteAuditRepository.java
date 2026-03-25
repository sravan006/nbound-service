package com.rxlink.inbound.router.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundRouteAuditRepository extends MongoRepository<InboundRouteAudit, String> {
}
