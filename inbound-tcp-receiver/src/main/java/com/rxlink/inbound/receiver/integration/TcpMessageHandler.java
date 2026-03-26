package com.rxlink.inbound.receiver.integration;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.receiver.service.ConnectionRegistryService;
import com.rxlink.inbound.receiver.service.CorrelationStore;
import com.rxlink.inbound.receiver.service.RouterClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class TcpMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(TcpMessageHandler.class);

    private final RouterClient routerClient;
    private final CorrelationStore correlationStore;
    private final ConnectionRegistryService connectionRegistryService;
    private final String sourceChannel;
    private final String correlationScope;
    private final Counter received;
    private final Counter forwarded;
    private final Counter forwardFailed;

    public TcpMessageHandler(
            RouterClient routerClient,
            CorrelationStore correlationStore,
            ConnectionRegistryService connectionRegistryService,
            @Value("${inbound.receiver.source-channel:TCP_RECEIVER}") String sourceChannel,
            @Value("${inbound.receiver.service-name:rxlink-inbound-receiver-7001}") String serviceName,
            MeterRegistry meterRegistry) {
        this.routerClient = routerClient;
        this.correlationStore = correlationStore;
        this.connectionRegistryService = connectionRegistryService;
        this.sourceChannel = sourceChannel;
        this.correlationScope = serviceName;
        this.received = Counter.builder("receiver_messages_received_total").tag("service_name", serviceName).register(meterRegistry);
        this.forwarded = Counter.builder("receiver_router_forward_success_total").tag("service_name", serviceName).register(meterRegistry);
        this.forwardFailed = Counter.builder("receiver_router_forward_failure_total").tag("service_name", serviceName).register(meterRegistry);
    }

    @ServiceActivator(inputChannel = "inboundTcpChannel")
    public byte[] onTcpMessage(Message<byte[]> message) {
        received.increment();
        byte[] bytes = message.getPayload();
        String correlationId = UUID.randomUUID().toString();
        String connectionId = Optional.ofNullable(message.getHeaders().get(IpHeaders.CONNECTION_ID, String.class)).orElse("unknown");
        connectionRegistryService.incrementTxn(connectionId);
        correlationStore.markReceived(correlationScope, correlationId, connectionId, bytes.length);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        InboundMessageDto dto = new InboundMessageDto(b64, correlationId, null, sourceChannel, Map.of());
        try {
            var response = routerClient.postRoute(dto);
            if ("ERROR".equals(response.status())) {
                throw new IllegalStateException(response.detail());
            }
            Optional<String> mappedConnectionId = correlationStore.getConnectionId(correlationScope, correlationId);
            if (mappedConnectionId.isEmpty() || !connectionId.equals(mappedConnectionId.get())) {
                throw new IllegalStateException("Correlation connection mismatch for " + correlationId);
            }
            byte[] tcpResponse = Base64.getDecoder().decode(Optional.ofNullable(response.responsePayloadBase64()).orElse(""));
            forwarded.increment();
            correlationStore.markForwarded(correlationScope, correlationId);
            return tcpResponse;
        } catch (Exception e) {
            forwardFailed.increment();
            correlationStore.markFailed(correlationScope, correlationId, e.getMessage());
            log.error("Forward to router failed correlationId={}", correlationId, e);
            return new byte[0];
        }
    }
}
