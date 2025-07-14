package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;

import java.util.Map;

public interface NodeExecutor<T extends BaseWorkflowNode> {
    ExecutionResult execute(T node, Map<String, Object> context);
}
