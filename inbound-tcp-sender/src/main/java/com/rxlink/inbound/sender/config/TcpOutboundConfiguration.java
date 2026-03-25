package com.rxlink.inbound.sender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class TcpOutboundConfiguration {

    @Bean
    public AbstractClientConnectionFactory rxClaimClientConnectionFactory(
            @Value("${inbound.sender.tcp.remote-host}") String host,
            @Value("${inbound.sender.tcp.remote-port}") int port,
            @Value("${inbound.sender.tcp.max-message-size:1048576}") int maxMessageSize,
            @Value("${inbound.sender.tcp.pool-size:4}") int poolSize) {
        TcpNetClientConnectionFactory tcp = new TcpNetClientConnectionFactory(host, port);
        ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer(maxMessageSize);
        tcp.setSerializer(serializer);
        tcp.setDeserializer(serializer);
        tcp.setSoTimeout(120_000);
        return new CachingClientConnectionFactory(tcp, poolSize);
    }

    @Bean
    public MessageChannel tcpOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "tcpOutboundChannel")
    public MessageHandler tcpSendingMessageHandler(AbstractClientConnectionFactory rxClaimClientConnectionFactory) {
        TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
        handler.setConnectionFactory(rxClaimClientConnectionFactory);
        handler.setClientMode(true);
        return handler;
    }
}
