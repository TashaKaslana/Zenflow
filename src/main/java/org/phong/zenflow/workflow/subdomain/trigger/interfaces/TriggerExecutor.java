package org.phong.zenflow.workflow.subdomain.trigger.interfaces;

import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

public interface TriggerExecutor extends PluginNodeExecutor {
    RunningHandle start(WorkflowTrigger trigger, TriggerContext ctx) throws Exception;

    interface RunningHandle {
        void stop();
    }
}
