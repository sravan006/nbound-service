package com.rxlink.inbound.receiver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
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
    public TcpReceivingChannelAdapter tcpReceivingChannelAdapter(
            TcpNetServerConnectionFactory tcpServerConnectionFactory,
            MessageChannel inboundTcpChannel) {
        TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
        adapter.setConnectionFactory(tcpServerConnectionFactory);
        adapter.setOutputChannel(inboundTcpChannel);
        adapter.setAutoStartup(true);
        return adapter;
    }
}
