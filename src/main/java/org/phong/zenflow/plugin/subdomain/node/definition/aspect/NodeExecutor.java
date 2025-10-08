package org.phong.zenflow.plugin.subdomain.node.definition.aspect;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;

public interface NodeExecutor {
    ExecutionResult execute(WorkflowConfig config, ExecutionContext context) throws Exception;
}
