package com.rxlink.inbound.sender.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SenderHttpConnectionMetricsFilter extends OncePerRequestFilter {

    private final AtomicInteger active = new AtomicInteger();

    public SenderHttpConnectionMetricsFilter(MeterRegistry meterRegistry) {
        Gauge.builder("sender_http_connections_active", active, AtomicInteger::get).register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        active.incrementAndGet();
        try {
            filterChain.doFilter(request, response);
        } finally {
            active.decrementAndGet();
        }
    }
}
