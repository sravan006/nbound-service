package com.rxlink.inbound.sender.web;

import com.rxlink.inbound.common.api.InboundMessageDto;
import com.rxlink.inbound.common.api.SendResponseDto;
import com.rxlink.inbound.sender.service.SendPipelineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<SendResponseDto> send(@Valid @RequestBody InboundMessageDto body) {
        SendResponseDto result = sendPipelineService.handle(body);
        if ("ERROR".equals(result.status())) {
            if (result.detail() != null && result.detail().contains("Invalid base64")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.accepted().body(result);
    }
}
