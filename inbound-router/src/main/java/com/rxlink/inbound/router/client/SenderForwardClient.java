package com.rxlink.inbound.router.client;

import com.rxlink.inbound.common.api.InboundMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SenderForwardClient {

    private final RestClient restClient;
    private final String sendPath;

    public SenderForwardClient(
            @Value("${inbound.router.sender.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${inbound.router.sender.read-timeout-ms:60000}") int readTimeoutMs,
            @Value("${inbound.router.sender.send-path:/api/v1/send}") String sendPath) {
        this.sendPath = sendPath;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().requestFactory(rf).build();
    }

    public void forward(String baseUrl, InboundMessageDto message) {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) + sendPath : baseUrl + sendPath;
        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(message)
                .retrieve()
                .toBodilessEntity();
    }
}
