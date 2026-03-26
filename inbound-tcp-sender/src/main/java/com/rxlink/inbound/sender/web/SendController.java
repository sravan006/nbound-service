package com.rxlink.inbound.sender.web;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.SendResponseDto;
import com.rxlink.inbound.sender.service.SendPipelineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SendController {

    private final SendPipelineService sendPipelineService;

    public SendController(SendPipelineService sendPipelineService) {
        this.sendPipelineService = sendPipelineService;
    }

    @PostMapping("/send")
    public ResponseEntity<SendResponseDto> send(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
            @Valid @RequestBody InboundMessageDto body) {
        InboundMessageDto request = body.correlationId() == null || body.correlationId().isBlank()
                ? new InboundMessageDto(body.payloadBase64(), correlationIdHeader, body.routingKey(), body.sourceChannel(), body.attributes())
                : body;
        SendResponseDto result = sendPipelineService.handle(request);
        if ("ERROR".equals(result.status())) {
            if (result.detail() != null && result.detail().contains("Invalid base64")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Correlation-Id", result.correlationId()).body(result);
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).header("X-Correlation-Id", result.correlationId()).body(result);
        }
        return ResponseEntity.accepted().header("X-Correlation-Id", result.correlationId()).body(result);
    }
}
