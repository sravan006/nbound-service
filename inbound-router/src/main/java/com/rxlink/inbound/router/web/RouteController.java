package com.rxlink.inbound.router.web;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.RouteResponseDto;
import com.rxlink.inbound.router.service.RoutingService;
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
public class RouteController {

    private final RoutingService routingService;

    public RouteController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/route")
    public ResponseEntity<RouteResponseDto> route(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationIdHeader,
            @Valid @RequestBody InboundMessageDto body) {
        InboundMessageDto request = body.correlationId() == null || body.correlationId().isBlank()
                ? new InboundMessageDto(body.payloadBase64(), correlationIdHeader, body.routingKey(), body.sourceChannel(), body.attributes())
                : body;
        RouteResponseDto result = routingService.route(request);
        if ("ERROR".equals(result.status())) {
            if (result.detail() != null && result.detail().contains("No route target")) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).header("X-Correlation-Id", result.correlationId()).body(result);
            }
            if (result.detail() != null && result.detail().contains("Invalid base64")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-Correlation-Id", result.correlationId()).body(result);
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).header("X-Correlation-Id", result.correlationId()).body(result);
        }
        return ResponseEntity.accepted().header("X-Correlation-Id", result.correlationId()).body(result);
    }
}
