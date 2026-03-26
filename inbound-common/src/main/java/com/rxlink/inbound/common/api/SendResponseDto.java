package com.rxlink.inbound.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendResponseDto(
        String correlationId,
        String status,
        String detail,
        String responsePayloadBase64
) {

    public static SendResponseDto accepted(String correlationId, String responsePayloadBase64) {
        return new SendResponseDto(correlationId, "SENT", null, responsePayloadBase64);
    }

    public static SendResponseDto error(String correlationId, String detail) {
        return new SendResponseDto(correlationId, "ERROR", detail, null);
    }
}
