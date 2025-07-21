package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PluginNodeExecutorDispatcher {

    private final PluginNodeExecutorRegistry registry;

    public ExecutionResult dispatch(PluginNode node, WorkflowConfig config) {
        switch (node.getExecutorType().toLowerCase()) {
            case "builtin" -> {
                String key = "core" + ":" + node.getKey();
                return registry.getExecutor(key)
                        .orElseThrow(() -> new ExecutorException("Executor not found: " + key))
                        .execute(config);
            }
            case "remote" -> {
                return registry.getExecutor("core:remote")
                        .orElseThrow(() -> new ExecutorException("Remote http executor not found"))
                        .execute(config);
            }
            default ->  throw new ExecutorException("Unknown executor type: " + node.getExecutorType());
        }
    }
}
