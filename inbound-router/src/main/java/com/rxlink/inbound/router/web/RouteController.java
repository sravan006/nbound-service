package com.rxlink.inbound.router.web;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.RouteResponseDto;
import com.rxlink.inbound.router.service.RoutingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<RouteResponseDto> route(@Valid @RequestBody InboundMessageDto body) {
        RouteResponseDto result = routingService.route(body);
        if ("ERROR".equals(result.status())) {
            if (result.detail() != null && result.detail().contains("No route target")) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
            }
            if (result.detail() != null && result.detail().contains("Invalid base64")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.accepted().body(result);
    }
}
