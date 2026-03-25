package com.rxlink.inbound.router.config;

import com.rxlink.inbound.router.service.RouteRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RouteRuleCacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(RouteRuleCacheInvalidationListener.class);

    private final RouteRuleService routeRuleService;

    public RouteRuleCacheInvalidationListener(RouteRuleService routeRuleService) {
        this.routeRuleService = routeRuleService;
    }

    @SuppressWarnings("unused")
    public void onMessage(String message, String pattern) {
        log.debug("Cache invalidation message pattern={} body={}", pattern, message);
        routeRuleService.evictAllLocalRouteCache();
    }
}
