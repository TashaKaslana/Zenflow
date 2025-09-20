package org.phong.zenflow.workflow.subdomain.trigger.events;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.springframework.context.ApplicationEvent;

@Getter
public class WorkflowTriggerRestartEvent extends ApplicationEvent {
    private final WorkflowTrigger trigger;

    public WorkflowTriggerRestartEvent(Object source, WorkflowTrigger trigger) {
        super(source);
        this.trigger = trigger;
    }
}
