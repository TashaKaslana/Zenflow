package org.phong.zenflow.plugin.subdomain.execution.interfaces;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;


public interface PluginNodeExecutor {
    // e.g., "Core:HTTP Request"
    String key();

    // execute the plugin node with the given configuration and context
    ExecutionResult execute(WorkflowConfig config);
}
