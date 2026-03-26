package com.rxlink.inbound.sender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.connection.DefaultTcpSSLContextSupport;
import org.springframework.integration.ip.tcp.connection.DefaultTcpNetSSLSocketFactorySupport;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.CachingClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayLengthHeaderSerializer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class TcpOutboundConfiguration {

    @Bean
    public AbstractClientConnectionFactory rxClaimClientConnectionFactory(
            @Value("${inbound.sender.tcp.remote-host}") String host,
            @Value("${inbound.sender.tcp.remote-port}") int port,
            @Value("${inbound.sender.tcp.max-message-size:1048576}") int maxMessageSize,
            @Value("${inbound.sender.tcp.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${inbound.sender.tcp.tls-enabled:false}") boolean tlsEnabled,
            @Value("${inbound.sender.tcp.tls.protocol:TLS}") String tlsProtocol,
            @Value("${inbound.sender.tcp.tls.keystore.path:}") String keyStorePath,
            @Value("${inbound.sender.tcp.tls.keystore.password:}") String keyStorePassword,
            @Value("${inbound.sender.tcp.tls.keystore.type:PKCS12}") String keyStoreType,
            @Value("${inbound.sender.tcp.tls.truststore.path:}") String trustStorePath,
            @Value("${inbound.sender.tcp.tls.truststore.password:}") String trustStorePassword,
            @Value("${inbound.sender.tcp.tls.truststore.type:PKCS12}") String trustStoreType,
            @Value("${inbound.sender.tcp.pool.min-connections:2}") int minConnections,
            @Value("${inbound.sender.tcp.pool.max-connections:8}") int maxConnections,
            @Value("${inbound.sender.tcp.pool.read-timeout-ms:120000}") int readTimeoutMs,
            MeterRegistry meterRegistry) {
        TcpNetClientConnectionFactory tcp = new TcpNetClientConnectionFactory(host, port);
        ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer(maxMessageSize);
        tcp.setSerializer(serializer);
        tcp.setDeserializer(serializer);
        tcp.setConnectTimeout(connectTimeoutMs);
        tcp.setSoTimeout(readTimeoutMs);
        if (tlsEnabled) {
            DefaultTcpSSLContextSupport sslContextSupport = new DefaultTcpSSLContextSupport(
                    keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
            sslContextSupport.setProtocol(tlsProtocol);
            if (keyStoreType != null && !keyStoreType.isBlank()) {
                sslContextSupport.setKeyStoreType(keyStoreType);
            }
            if (trustStoreType != null && !trustStoreType.isBlank()) {
                sslContextSupport.setTrustStoreType(trustStoreType);
            }
            tcp.setTcpSocketFactorySupport(new DefaultTcpNetSSLSocketFactorySupport(sslContextSupport));
        }
        CachingClientConnectionFactory cache = new CachingClientConnectionFactory(tcp, maxConnections);
        cache.setPoolSize(maxConnections);
        cache.setConnectionWaitTimeout(readTimeoutMs);
        Gauge.builder("sender_rxclaim_pool_active", cache, c -> invokeInt(c, "getActiveCount"))
                .register(meterRegistry);
        Gauge.builder("sender_rxclaim_pool_idle", cache, c -> invokeInt(c, "getIdleCount"))
                .register(meterRegistry);
        Gauge.builder("sender_rxclaim_pool_allocated", cache, c -> invokeInt(c, "getAllocatedCount"))
                .register(meterRegistry);
        return cache;
    }

    @Bean
    public MessageChannel tcpOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel tcpOutboundReplyChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "tcpOutboundChannel", outputChannel = "tcpOutboundReplyChannel")
    public MessageHandler tcpOutboundGateway(
            AbstractClientConnectionFactory rxClaimClientConnectionFactory,
            @Value("${inbound.sender.tcp.pool.read-timeout-ms:120000}") long readTimeoutMs) {
        TcpOutboundGateway gateway = new TcpOutboundGateway();
        gateway.setConnectionFactory(rxClaimClientConnectionFactory);
        gateway.setRemoteTimeout(readTimeoutMs);
        return gateway;
    }

    private static double invokeInt(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return value instanceof Number n ? n.doubleValue() : 0d;
        } catch (Exception ignored) {
            return 0d;
        }
    }
}
