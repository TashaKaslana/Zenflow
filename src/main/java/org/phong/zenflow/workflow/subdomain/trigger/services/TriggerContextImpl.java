package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@AllArgsConstructor
public class TriggerContextImpl implements TriggerContext {
    private final ApplicationEventPublisher publisher;
    private final WorkflowTriggerRepository repo;

    @Override
    public void startWorkflow(UUID workflowId, UUID triggerExecutorId, Map<String, Object> payload) {
        publisher.publishEvent(new WorkflowTriggerEvent(
                UUID.randomUUID(),
                TriggerType.EVENT,
                triggerExecutorId,
                workflowId,
                new WorkflowRunnerRequest(
                        null,
                        null,
                        payload
                )
        ));
    }

    @Override
    @Transactional
    public void markTriggered(UUID triggerId, Instant at) {
        repo.updateLastTriggeredAt(triggerId, at);
    }
}