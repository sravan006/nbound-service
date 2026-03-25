package com.rxlink.inbound.sender.config;

import com.rxlink.inbound.sender.orchestration.OrchestrationRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SenderOrchestrationInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(SenderOrchestrationInvalidationListener.class);

    private final OrchestrationRuleService orchestrationRuleService;

    public SenderOrchestrationInvalidationListener(OrchestrationRuleService orchestrationRuleService) {
        this.orchestrationRuleService = orchestrationRuleService;
    }

    @SuppressWarnings("unused")
    public void onMessage(String message, String pattern) {
        log.debug("Orchestration cache invalidation: {}", message);
        orchestrationRuleService.clearLocalCache();
    }
}
