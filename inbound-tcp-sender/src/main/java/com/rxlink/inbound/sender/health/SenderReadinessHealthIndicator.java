package com.rxlink.inbound.sender.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;

@Component("senderReadiness")
public class SenderReadinessHealthIndicator implements HealthIndicator {

    private final boolean checkRxClaim;
    private final String host;
    private final int port;
    private final int connectTimeoutMs;

    public SenderReadinessHealthIndicator(
            @Value("${inbound.sender.readiness.check-rxclaim-enabled:false}") boolean checkRxClaim,
            @Value("${inbound.sender.tcp.remote-host}") String host,
            @Value("${inbound.sender.tcp.remote-port}") int port,
            @Value("${inbound.sender.tcp.connect-timeout-ms:5000}") int connectTimeoutMs) {
        this.checkRxClaim = checkRxClaim;
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    @Override
    public Health health() {
        if (!checkRxClaim) {
            return Health.up().withDetail("rxclaimCheck", "disabled").build();
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            return Health.up().withDetail("rxclaim", "reachable").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("rxclaim", "unreachable").build();
        }
    }
}
