package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.dto.RuntimeMetadata;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for executing a single plugin node outside a workflow context.
 * This mimics the execution behavior of a node within a workflow by
 * performing runtime validation and dispatching through the standard
 * executor dispatcher.
 */
@Service
@AllArgsConstructor
@Slf4j
public class SingleNodeExecutionService {

    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final WorkflowValidationService workflowValidationService;

    /**
     * Execute a plugin node with the provided configuration.
     *
     * @param plugin the plugin node to execute
     * @param config the configuration for the node
     * @return the {@link ExecutionResult} returned by the node's executor
     */
    public ExecutionResult executeNode(PluginNode plugin, WorkflowConfig config) {
        // Initialize runtime context similar to workflow runs
        RuntimeContext context = new RuntimeContext();
        context.initialize(Map.of(), Map.of(), Map.of());
        UUID runId = UUID.randomUUID();
        RuntimeContextPool.registerContext(runId, context);

        WorkflowConfig safeConfig = (config != null) ? config : new WorkflowConfig();
        WorkflowConfig resolvedConfig = context.resolveConfig(plugin.getKey(), safeConfig);

        PluginNodeIdentifier identifier = new PluginNodeIdentifier(
                plugin.getPlugin().getKey(),
                plugin.getKey(),
                plugin.getPluginNodeVersion(),
                plugin.getExecutorType()
        );

        ValidationResult validationResult = workflowValidationService.validateRuntime(
                plugin.getKey(),
                resolvedConfig,
                identifier.toCacheKey(),
                runId
        );

        if (!validationResult.isValid()) {
            log.warn("Validation failed for node {}: {}", plugin.getKey(), validationResult.getErrors());
            return ExecutionResult.validationError(validationResult, plugin.getKey());
        }

        ExecutionInput input = new ExecutionInput(resolvedConfig, new RuntimeMetadata(runId));
        try {
            return executorDispatcher.dispatch(identifier, input);
        } finally {
            RuntimeContextPool.removeContext(runId);
        }
    }
}

