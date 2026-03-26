package com.rxlink.inbound.sender.tcp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TcpDispatchService {

    private final MessageChannel tcpOutboundChannel;
    private final MessageChannel tcpOutboundReplyChannel;
    private final GenericMessagingTemplate messagingTemplate = new GenericMessagingTemplate();

    public TcpDispatchService(
            @Qualifier("tcpOutboundChannel") MessageChannel tcpOutboundChannel,
            @Qualifier("tcpOutboundReplyChannel") MessageChannel tcpOutboundReplyChannel) {
        this.tcpOutboundChannel = tcpOutboundChannel;
        this.tcpOutboundReplyChannel = tcpOutboundReplyChannel;
        this.messagingTemplate.setReceiveTimeout(120_000);
    }

    public byte[] sendAndReceive(byte[] payload) {
        Message<?> response = messagingTemplate.sendAndReceive(
                tcpOutboundChannel,
                MessageBuilder.withPayload(payload).setReplyChannel(tcpOutboundReplyChannel).build());
        if (response == null || !(response.getPayload() instanceof byte[] bytes)) {
            return new byte[0];
        }
        return bytes;
    }
}
