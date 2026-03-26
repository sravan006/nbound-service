package com.rxlink.inbound.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteResponseDto(
        String correlationId,
        String routingKey,
        String targetSenderUrl,
        String status,
        String detail,
        String responsePayloadBase64
) {
    public static RouteResponseDto ok(
            String correlationId,
            String routingKey,
            String targetSenderUrl,
            String responsePayloadBase64) {
        return new RouteResponseDto(correlationId, routingKey, targetSenderUrl, "ACCEPTED", null, responsePayloadBase64);
    }

    public static RouteResponseDto error(String correlationId, String detail) {
        return new RouteResponseDto(correlationId, null, null, "ERROR", detail, null);
    }
}
