package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PluginNodeExecutorDispatcher {

    private final PluginNodeExecutorRegistry registry;

    public ExecutionResult dispatch(PluginNodeIdentifier identifier, ExecutionInput input) {
        switch (identifier.executorType().toLowerCase()) {
            case "builtin" -> {
                String key = identifier.toString();
                return registry.getExecutor(key)
                        .orElseThrow(() -> new ExecutorException("Executor not found: " + key))
                        .execute(input);
            }
            case "remote" -> {
                return registry.getExecutor("core:remote:1.0.0")
                        .orElseThrow(() -> new ExecutorException("Remote http executor not found"))
                        .execute(input);
            }
            default ->  throw new ExecutorException("Unknown executor type: " + identifier.executorType());
        }
    }
}
