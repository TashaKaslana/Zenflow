package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PluginNodeExecutorDispatcher {

    private final PluginNodeExecutorRegistry registry;

    public ExecutionResult dispatch(PluginNodeIdentifier identifier, WorkflowConfig config, RuntimeContext context) {
        switch (identifier.executorType().toLowerCase()) {
            case "builtin" -> {
                return registry.getExecutor(identifier)
                        .orElseThrow(() -> new ExecutorException("Executor not found: " + identifier))
                        .execute(config, context);
            }
            case "remote" -> {
                PluginNodeIdentifier remoteId = new PluginNodeIdentifier("core", "remote", "1.0.0", null);
                return registry.getExecutor(remoteId)
                        .orElseThrow(() -> new ExecutorException("Remote http executor not found"))
                        .execute(config, context);
            }
            default ->  throw new ExecutorException("Unknown executor type: " + identifier.executorType());
        }
    }
}
