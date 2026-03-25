package com.rxlink.inbound.sender.tcp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
public class TcpDispatchService {

    private final MessageChannel tcpOutboundChannel;

    public TcpDispatchService(@Qualifier("tcpOutboundChannel") MessageChannel tcpOutboundChannel) {
        this.tcpOutboundChannel = tcpOutboundChannel;
    }

    public void dispatch(byte[] payload) {
        tcpOutboundChannel.send(MessageBuilder.withPayload(payload).build());
    }
}
