package org.phong.zenflow.plugin.subdomain.resource.trigger;

import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

public abstract class BaseTriggerResourceManager<T, C extends ResourceConfig> extends BaseNodeResourceManager<T, C> {
    @Override
    public boolean isManual() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        return (C) new VoidResourceConfig();
    }
}
