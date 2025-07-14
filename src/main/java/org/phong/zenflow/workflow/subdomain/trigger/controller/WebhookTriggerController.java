package org.phong.zenflow.workflow.subdomain.trigger.controller;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.trigger.services.WebhookTriggerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@AllArgsConstructor
public class WebhookTriggerController {

    private final WebhookTriggerService webhookTriggerService;

    @PostMapping("/{identifier}")
    public ResponseEntity<?> triggerWebhook(@PathVariable String identifier,
                                            @RequestBody Map<String, Object> payload,
                                            @RequestHeader(value = "X-Signature", required = false) String signature) {
        UUID runId = webhookTriggerService.trigger(identifier, payload, signature);
        return ResponseEntity.accepted().body(Map.of("runId", runId));
    }
}
