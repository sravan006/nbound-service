package com.rxlink.inbound.receiver.config;

import com.rxlink.inbound.receiver.service.ConnectionRegistryService;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.stereotype.Component;

@Component
public class TcpConnectionEventsListener implements ApplicationListener<TcpConnectionEvent> {

    private final ConnectionRegistryService connectionRegistryService;

    public TcpConnectionEventsListener(ConnectionRegistryService connectionRegistryService) {
        this.connectionRegistryService = connectionRegistryService;
    }

    @Override
    public void onApplicationEvent(TcpConnectionEvent event) {
        if (event instanceof TcpConnectionOpenEvent openEvent) {
            Object src = openEvent.getSource();
            if (src instanceof TcpConnection connection) {
                connectionRegistryService.onOpen(connection);
            }
            return;
        }
        if (event instanceof TcpConnectionCloseEvent) {
            connectionRegistryService.onClose(event.getConnectionId());
        }
    }
}
