package com.rxlink.inbound.receiver.service;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.RouteResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RouterClient {

    private final RestClient restClient;
    private final String routePath;

    public RouterClient(
            @Value("${inbound.receiver.router.base-url}") String routerBaseUrl,
            @Value("${inbound.receiver.router.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${inbound.receiver.router.read-timeout-ms:120000}") int readTimeoutMs,
            @Value("${inbound.receiver.router.route-path:/api/v1/route}") String routePath) {
        this.routePath = routePath;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        String base = routerBaseUrl.endsWith("/")
                ? routerBaseUrl.substring(0, routerBaseUrl.length() - 1)
                : routerBaseUrl;
        this.restClient = RestClient.builder().baseUrl(base).requestFactory(rf).build();
    }

    public RouteResponseDto postRoute(InboundMessageDto body) {
        return restClient.post()
                .uri(routePath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(RouteResponseDto.class);
    }
}
