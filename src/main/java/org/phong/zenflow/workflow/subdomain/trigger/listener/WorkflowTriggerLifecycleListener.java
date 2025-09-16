package org.phong.zenflow.workflow.subdomain.trigger.listener;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.workflow.subdomain.trigger.events.WorkflowTriggerRestartEvent;
import org.phong.zenflow.workflow.subdomain.trigger.events.WorkflowTriggerStartEvent;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.services.TriggerOrchestrator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WorkflowTriggerLifecycleListener {

    private final TriggerOrchestrator triggerOrchestrator;

    @TransactionalEventListener
    public void handleWorkflowTriggerStart(WorkflowTriggerStartEvent event) {
        try {
            triggerOrchestrator.start(event.getTrigger());
        } catch (Exception ignored) { }
    }

    @TransactionalEventListener
    public void handleWorkflowTriggerRestart(WorkflowTriggerRestartEvent event) {
        WorkflowTrigger trigger = event.getTrigger();
        try {
            triggerOrchestrator.stop(trigger);
            if (Boolean.TRUE.equals(trigger.getEnabled())) {
                triggerOrchestrator.start(trigger);
            }
        } catch (Exception ignored) { }
    }
}
