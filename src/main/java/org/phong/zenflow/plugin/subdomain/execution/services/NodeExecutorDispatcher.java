package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.ExternalPluginExecutor;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class NodeExecutorDispatcher {

    private final PluginNodeExecutorRegistry registry;

    public ExecutionResult dispatch(String identifier, String executorType, WorkflowConfig config, ExecutionContext context) {
        NodeDefinition definition;
        switch (executorType.toLowerCase()) {
            case "builtin" -> {
                definition = registry.getDefinition(identifier)
                        .orElseThrow(() -> new ExecutorException("Executor not found: " + identifier));
            }
            case "remote" -> {
                log.warn("Placeholder execute dispatch!");
                definition = registry.getDefinition(identifier)
                        .orElseThrow(() -> new ExecutorException("Remote http executor not found"));
            }
            default -> throw new ExecutorException("Unknown executor type: " + executorType);
        }

        NodeExecutor executor = definition.getNodeExecutor();
        if (executor instanceof ExternalPluginExecutor external) {
            LogContext ctx = LogContextManager.snapshot();
            external.setLogContext(ctx);
        }

        try {
            return executor.execute(config, context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
