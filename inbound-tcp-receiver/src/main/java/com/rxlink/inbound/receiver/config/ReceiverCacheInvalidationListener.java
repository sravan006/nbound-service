package com.rxlink.inbound.receiver.config;

import com.rxlink.inbound.receiver.service.ReceiverConfigService;
import org.springframework.stereotype.Component;

@Component
public class ReceiverCacheInvalidationListener {

    private final ReceiverConfigService receiverConfigService;

    public ReceiverCacheInvalidationListener(ReceiverConfigService receiverConfigService) {
        this.receiverConfigService = receiverConfigService;
    }

    @SuppressWarnings("unused")
    public void onMessage(String message, String pattern) {
        receiverConfigService.clearLocalCache();
    }
}
