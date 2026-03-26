package com.rxlink.inbound.receiver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import org.springframework.messaging.MessageChannel;

@Configuration
public class TcpInboundConfiguration {

    @Bean
    public TcpNetServerConnectionFactory tcpServerConnectionFactory(
            @Value("${inbound.receiver.tcp.port}") int port,
            @Value("${inbound.receiver.tcp.max-message-size:1048576}") int maxMessageSize) {
        TcpNetServerConnectionFactory factory = new TcpNetServerConnectionFactory(port);
        ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer(maxMessageSize);
        factory.setSerializer(serializer);
        factory.setDeserializer(serializer);
        return factory;
    }

    @Bean
    public MessageChannel inboundTcpChannel() {
        return new DirectChannel();
    }

    @Bean
    public TcpInboundGateway tcpInboundGateway(
            TcpNetServerConnectionFactory tcpServerConnectionFactory,
            MessageChannel inboundTcpChannel) {
        TcpInboundGateway gateway = new TcpInboundGateway();
        gateway.setConnectionFactory(tcpServerConnectionFactory);
        gateway.setRequestChannel(inboundTcpChannel);
        gateway.setReplyTimeout(120_000);
        gateway.setRequestTimeout(120_000);
        return gateway;
    }
}
