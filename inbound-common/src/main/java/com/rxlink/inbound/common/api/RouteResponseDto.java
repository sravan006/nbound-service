package com.rxlink.inbound.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RouteResponseDto(
        String correlationId,
        String routingKey,
        String targetSenderUrl,
        String status,
        String detail
) {
    public static RouteResponseDto ok(String correlationId, String routingKey, String targetSenderUrl) {
        return new RouteResponseDto(correlationId, routingKey, targetSenderUrl, "ACCEPTED", null);
    }

    public static RouteResponseDto error(String correlationId, String detail) {
        return new RouteResponseDto(correlationId, null, null, "ERROR", detail);
    }
}
