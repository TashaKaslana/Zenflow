package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;

import java.util.Map;

public interface NodeExecutor<T extends BaseWorkflowNode> {
    String getNodeType();

    ExecutionResult execute(T node, Map<String, Object> context);

    default ExecutionResult executeGeneric(BaseWorkflowNode node, Map<String, Object> context) {
        @SuppressWarnings("unchecked")
        T casted = (T) node;
        return execute(casted, context);
    }
}
