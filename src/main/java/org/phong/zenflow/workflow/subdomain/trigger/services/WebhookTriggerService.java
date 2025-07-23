package org.phong.zenflow.workflow.subdomain.trigger.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.utils.HmacUtils;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.WorkflowStatus;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.repository.WorkflowRunRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WebhookTriggerService {

    private final WorkflowTriggerRepository triggerRepo;
    private final WorkflowRunRepository runRepo;
    private final WorkflowRepository workflowRepo;
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper;

    public UUID trigger(String identifier, Map<String, Object> payload, String signature) {
        WorkflowTrigger trigger = resolveTrigger(identifier);

        JsonNode config = (JsonNode) trigger.getConfig();
        if (config.has("secret")) {
            String secret = config.get("secret").asText();
            String rawBody = toJson(payload);
            HmacUtils.verifySignatureOrThrow(secret, rawBody, signature);
        }

        UUID workflowId = trigger.getWorkflowId();
        Workflow workflow = workflowRepo.getReferenceById(workflowId);
        String callbackUrl = config.has("callbackUrl") ? config.get("callbackUrl").asText() : null;

        WorkflowRun run = new WorkflowRun();
        run.setWorkflow(workflow);
        run.setTriggerType(TriggerType.WEBHOOK);
        run.setStatus(WorkflowStatus.RUNNING);
        run.setStartedAt(OffsetDateTime.now());

        run = runRepo.save(run);

        WorkflowRun finalRun = run;
        eventPublisher.publishEvent(new WorkflowRunnerPublishableEvent() {
            @Override
            public UUID getWorkflowRunId() {
                return finalRun.getId();
            }

            @Override
            public TriggerType getTriggerType() {
                return TriggerType.WEBHOOK;
            }

            @Override
            public UUID getWorkflowId() {
                return workflowId;
            }

            @Override
            public WorkflowRunnerRequest request() {
                return new WorkflowRunnerRequest(callbackUrl, null);
            }
        });

        return run.getId();
    }

    private WorkflowTrigger resolveTrigger(String identifier) {
        Optional<WorkflowTrigger> optionalTrigger = isUUID(identifier)
            ? triggerRepo.findById(UUID.fromString(identifier))
            : triggerRepo.findByCustomPath(identifier); // from config.custom_path

        return optionalTrigger
            .filter(t -> t.getType() == TriggerType.WEBHOOK && t.getEnabled())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook trigger not found or disabled"));
    }

    private boolean isUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }
}
