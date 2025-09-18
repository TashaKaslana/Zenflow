package org.phong.zenflow.workflow.subdomain.trigger.listener;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.workflow.event.WorkflowDefinitionUpdatedEvent;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.trigger.events.WorkflowTriggerRestartEvent;
import org.phong.zenflow.workflow.subdomain.trigger.events.WorkflowTriggerStartEvent;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.services.TriggerOrchestrator;
import org.phong.zenflow.workflow.subdomain.trigger.services.WorkflowTriggerService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WorkflowTriggerListener {

    private final TriggerOrchestrator triggerOrchestrator;
    private final WorkflowTriggerService triggerService;

    @TransactionalEventListener
    @Async
    public void handleWorkflowTriggerStart(WorkflowTriggerStartEvent event) {
        try {
            triggerOrchestrator.start(event.getTrigger());
        } catch (Exception ignored) { }
    }

    @TransactionalEventListener
    @Async
    public void handleWorkflowTriggerRestart(WorkflowTriggerRestartEvent event) {
        WorkflowTrigger trigger = event.getTrigger();
        try {
            triggerOrchestrator.stop(trigger);
            if (Boolean.TRUE.equals(trigger.getEnabled())) {
                triggerOrchestrator.start(trigger);
            }
        } catch (Exception ignored) { }
    }

    @Async
    @TransactionalEventListener
    public void onWorkflowDefinitionUpdated(WorkflowDefinitionUpdatedEvent event) {
        WorkflowDefinition definition = event.definition();
        if (definition != null) {
            triggerService.synchronizeTrigger(event.workflowId(), definition);
        }
    }
}
