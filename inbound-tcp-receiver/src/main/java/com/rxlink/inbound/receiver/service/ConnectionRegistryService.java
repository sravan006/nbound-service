package com.rxlink.inbound.receiver.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConnectionRegistryService {

    private final ReceiverConfigService receiverConfigService;
    private final AtomicInteger activeCount = new AtomicInteger();
    private final Map<String, ConnectionInfo> registry = new ConcurrentHashMap<>();
    private final Counter rejectedCounter;
    private final Gauge utilizationGauge;

    public ConnectionRegistryService(
            ReceiverConfigService receiverConfigService,
            @org.springframework.beans.factory.annotation.Value("${inbound.receiver.service-name:rxlink-inbound-receiver-7001}") String serviceName,
            MeterRegistry meterRegistry) {
        this.receiverConfigService = receiverConfigService;
        this.rejectedCounter = Counter.builder("receiver_tcp_connection_rejected_total")
                .tag("service_name", serviceName)
                .register(meterRegistry);
        Gauge.builder("receiver_tcp_connections_active", activeCount, AtomicInteger::get)
                .tag("service_name", serviceName)
                .register(meterRegistry);
        this.utilizationGauge = Gauge.builder("receiver_tcp_connection_utilization", this, ConnectionRegistryService::utilization)
                .tag("service_name", serviceName)
                .register(meterRegistry);
    }

    public void onOpen(TcpConnection connection) {
        int current = activeCount.incrementAndGet();
        ConnectionInfo info = new ConnectionInfo();
        info.connectedAt = Instant.now().toString();
        info.clientIp = connection.getHostAddress();
        info.clientPort = connection.getPort();
        registry.put(connection.getConnectionId(), info);
        if (current > receiverConfigService.maxConnections()) {
            rejectedCounter.increment();
            registry.remove(connection.getConnectionId());
            activeCount.decrementAndGet();
            connection.close();
        }
    }

    public void onClose(String connectionId) {
        if (registry.remove(connectionId) != null) {
            activeCount.decrementAndGet();
        }
    }

    public void incrementTxn(String connectionId) {
        ConnectionInfo info = registry.get(connectionId);
        if (info != null) {
            info.txnCount.incrementAndGet();
        }
    }

    private double utilization() {
        int max = Math.max(1, receiverConfigService.maxConnections());
        return activeCount.get() / (double) max;
    }

    private static final class ConnectionInfo {
        private String clientIp;
        private int clientPort;
        private String connectedAt;
        private final AtomicLong txnCount = new AtomicLong();
    }
}
