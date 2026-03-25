package com.rxlink.inbound.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InboundMessageDto(
        @NotBlank String payloadBase64,
        String correlationId,
        String routingKey,
        String sourceChannel,
        Map<String, String> attributes
) {
}
