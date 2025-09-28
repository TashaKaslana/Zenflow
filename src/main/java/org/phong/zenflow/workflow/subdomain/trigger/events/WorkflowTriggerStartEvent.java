package org.phong.zenflow.workflow.subdomain.trigger.events;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.springframework.context.ApplicationEvent;

@Getter
public class WorkflowTriggerStartEvent extends ApplicationEvent {
    private final WorkflowTrigger trigger;

    public WorkflowTriggerStartEvent(Object source, WorkflowTrigger trigger) {
        super(source);
        this.trigger = trigger;
    }
}
