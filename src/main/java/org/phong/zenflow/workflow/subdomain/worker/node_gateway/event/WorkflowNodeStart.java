package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.dtos.BaseTaskEnvelope;

import java.util.UUID;

@Getter
public class WorkflowNodeStart extends BaseTaskEnvelope {
    public WorkflowNodeStart(UUID workflowRunId, PluginNodeIdentifier pluginNodeIdentifier) {
        super(workflowRunId);
        this.pluginNodeIdentifier = pluginNodeIdentifier;
    }

    private final PluginNodeIdentifier pluginNodeIdentifier;
}
