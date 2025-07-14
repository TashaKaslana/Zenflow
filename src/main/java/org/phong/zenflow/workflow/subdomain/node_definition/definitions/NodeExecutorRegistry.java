package org.phong.zenflow.workflow.subdomain.node_definition.definitions;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NodeExecutorRegistry {
    private final Map<String, NodeExecutor<?>> executors;

    public NodeExecutorRegistry(List<NodeExecutor<?>> executors) {
        this.executors = executors.stream().collect(Collectors.toMap(NodeExecutor::getNodeType, map -> map));
    }

    public Optional<NodeExecutor<?>> getExecutor(String type) {
        return Optional.ofNullable(executors.get(type));
    }

    public ExecutionResult execute(BaseWorkflowNode node, Map<String, Object> context) {
        NodeExecutor<?> executor = executors.get(node.getType().name());
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for node type: " + node.getType());
        }
        return executor.executeGeneric(node, context);
    }
}
