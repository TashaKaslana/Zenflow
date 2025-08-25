package org.phong.zenflow.workflow.subdomain.trigger.interfaces;

import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

public interface TriggerExecutor {
    RunningHandle start(WorkflowTrigger trigger, TriggerContext ctx) throws Exception;

    interface RunningHandle {
        void stop();
    }
}
