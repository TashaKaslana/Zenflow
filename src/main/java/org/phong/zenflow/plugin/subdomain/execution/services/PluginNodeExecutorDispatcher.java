package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.ExternalPluginExecutor;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PluginNodeExecutorDispatcher {

    private final PluginNodeExecutorRegistry registry;

    public ExecutionResult dispatch(PluginNodeIdentifier identifier, WorkflowConfig config, ExecutionContext context) {
        PluginNodeExecutor executor;
        switch (identifier.executorType().toLowerCase()) {
            case "builtin" -> {
                executor = registry.getExecutor(identifier)
                        .orElseThrow(() -> new ExecutorException("Executor not found: " + identifier));
            }
            case "remote" -> {
                PluginNodeIdentifier remoteId = new PluginNodeIdentifier("core", "remote", "1.0.0", null);
                executor = registry.getExecutor(remoteId)
                        .orElseThrow(() -> new ExecutorException("Remote http executor not found"));
            }
            default -> throw new ExecutorException("Unknown executor type: " + identifier.executorType());
        }

        if (executor instanceof ExternalPluginExecutor external) {
            LogContext ctx = LogContextManager.snapshot();
            external.setLogContext(ctx);
        }
        return executor.execute(config, context);
    }
}
