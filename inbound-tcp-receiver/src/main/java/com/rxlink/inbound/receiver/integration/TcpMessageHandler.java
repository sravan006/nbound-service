package com.rxlink.inbound.receiver.integration;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.receiver.service.CorrelationStore;
import com.rxlink.inbound.receiver.service.RouterClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class TcpMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(TcpMessageHandler.class);

    private final RouterClient routerClient;
    private final CorrelationStore correlationStore;
    private final String sourceChannel;
    private final Counter received;
    private final Counter forwarded;
    private final Counter forwardFailed;

    public TcpMessageHandler(
            RouterClient routerClient,
            CorrelationStore correlationStore,
            @Value("${inbound.receiver.source-channel:TCP_RECEIVER}") String sourceChannel,
            MeterRegistry meterRegistry) {
        this.routerClient = routerClient;
        this.correlationStore = correlationStore;
        this.sourceChannel = sourceChannel;
        this.received = Counter.builder("inbound.receiver.tcp.messages.received").register(meterRegistry);
        this.forwarded = Counter.builder("inbound.receiver.router.forward.success").register(meterRegistry);
        this.forwardFailed = Counter.builder("inbound.receiver.router.forward.failure").register(meterRegistry);
    }

    @ServiceActivator(inputChannel = "inboundTcpChannel")
    public void onTcpMessage(Message<byte[]> message) {
        received.increment();
        byte[] bytes = message.getPayload();
        String correlationId = UUID.randomUUID().toString();
        correlationStore.markReceived(correlationId, bytes.length);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        InboundMessageDto dto = new InboundMessageDto(b64, correlationId, null, sourceChannel, Map.of());
        try {
            routerClient.postRoute(dto);
            forwarded.increment();
            correlationStore.markForwarded(correlationId);
        } catch (Exception e) {
            forwardFailed.increment();
            correlationStore.markFailed(correlationId, e.getMessage());
            log.error("Forward to router failed correlationId={}", correlationId, e);
        }
    }
}
