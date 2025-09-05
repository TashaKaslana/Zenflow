package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class TriggerContextImpl implements TriggerContext {
    private final ApplicationEventPublisher publisher;
    private final TriggerAsyncService triggerAsyncService;

    public TriggerContextImpl(ApplicationEventPublisher publisher,
                              TriggerAsyncService triggerAsyncService) {
        this.publisher = publisher;
        this.triggerAsyncService = triggerAsyncService;
    }

    @Override
    public void startWorkflow(UUID workflowId, UUID triggerExecutorId, Map<String, Object> payload) {
        log.debug("Publishing WorkflowTriggerEvent for workflow {}", workflowId);
        publisher.publishEvent(
                new WorkflowTriggerEvent(
                        UUID.randomUUID(),
                        TriggerType.EVENT,
                        triggerExecutorId,
                        workflowId,
                        new WorkflowRunnerRequest(null, null, payload)
                )
        );
    }

    @Override
    public void markTriggered(UUID triggerId, Instant at) {
        triggerAsyncService.markTriggeredAsync(triggerId, at);
    }
}