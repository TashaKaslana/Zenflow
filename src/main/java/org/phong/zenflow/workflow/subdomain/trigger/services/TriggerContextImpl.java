package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@AllArgsConstructor
public class TriggerContextImpl implements TriggerContext {
    private final ApplicationEventPublisher publisher;
    private final WorkflowTriggerRepository repo;

    @Override
    public void startWorkflow(UUID workflowId, Map<String, Object> payload) {
        publisher.publishEvent(new WorkflowRunnerPublishableEvent() {
            @Override
            public UUID getWorkflowRunId() {
                return UUID.randomUUID();
            }

            @Override
            public TriggerType getTriggerType() {
                return TriggerType.EVENT;
            }

            @Override
            public UUID getWorkflowId() {
                return workflowId;
            }

            @Override
            public WorkflowRunnerRequest request() {
                return null;
            }
        });
    }

    @Override
    public void markTriggered(UUID triggerId, Instant at) {
        repo.findById(triggerId).ifPresent(t -> {
            t.setLastTriggeredAt(OffsetDateTime.from(at));
            repo.save(t);
        });
    }
}
