package com.rxlink.inbound.receiver.service;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.RouteResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RouterClient {

    private final ReceiverConfigService receiverConfigService;

    public RouterClient(ReceiverConfigService receiverConfigService) {
        this.receiverConfigService = receiverConfigService;
    }

    public RouteResponseDto postRoute(InboundMessageDto body) {
        String routerBaseUrl = receiverConfigService.routerBaseUrl();
        int connectTimeoutMs = receiverConfigService.routerConnectTimeoutMs();
        int readTimeoutMs = receiverConfigService.routerReadTimeoutMs();
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        String base = routerBaseUrl.endsWith("/")
                ? routerBaseUrl.substring(0, routerBaseUrl.length() - 1)
                : routerBaseUrl;
        RestClient restClient = RestClient.builder().baseUrl(base).requestFactory(rf).build();
        return restClient.post()
                .uri(receiverConfigService.routePath())
                .header("X-Correlation-Id", body.correlationId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RouteResponseDto.class);
    }
}
