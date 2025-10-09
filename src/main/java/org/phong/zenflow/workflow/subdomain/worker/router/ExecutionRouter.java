package org.phong.zenflow.workflow.subdomain.worker.router;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class ExecutionRouter {
    private final PluginNodeService pluginNodeService;

    public ExecutionRoute route(ExecutionContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Execution context must not be null");
        }

        UUID pluginNodeId = context.getPluginNodeId();
        if (pluginNodeId == null) {
            throw new IllegalStateException("Execution context is missing plugin node identifier");
        }

        WorkflowConfig config = context.getCurrentConfig();
        if (config == null) {
            throw new IllegalStateException("Execution context is missing resolved workflow configuration");
        }

        PluginNode pluginNode = pluginNodeService.findById(pluginNodeId);
        String executorType = pluginNode.getExecutorType();
        if (executorType == null || executorType.isBlank()) {
            throw new IllegalStateException("Executor type is not defined for plugin node: " + pluginNodeId);
        }

        return new ExecutionRoute(pluginNodeId.toString(), executorType, config);
    }

    public record ExecutionRoute(String identifier,
                                 String executorType,
                                 WorkflowConfig config) {
    }
}
