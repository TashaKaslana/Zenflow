package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.SystemWorkflowStateKeyBuilder;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@AllArgsConstructor
public class NodeExecutionMediator {
    private final PluginNodeExecutorDispatcher executorDispatcher;

    // System-state-aware node types
    private static final Set<String> SYSTEM_STATE_NODES = Set.of(
            "core:flow.loop.for",
            "core:flow.loop.while",
            "core:flow.loop.foreach",
            "core:flow.condition.retry"
    );

    public ExecutionResult executeWithStateManagement(PluginNode pluginNode, WorkflowConfig config,
                                                      String nodeKey, RuntimeContext runtimeContext) {
        String pluginKey = "core:" + pluginNode.getKey();

        // Check if this node requires system state management
        if (SYSTEM_STATE_NODES.contains(pluginKey)) {
            return executeStatefulNode(pluginNode, config, nodeKey, pluginKey, runtimeContext);
        } else {
            return executorDispatcher.dispatch(pluginNode, config);
        }
    }

    private ExecutionResult executeStatefulNode(PluginNode pluginNode, WorkflowConfig config,
                                                String nodeKey, String pluginKey, RuntimeContext runtimeContext) {
        // Pre-execution: inject system state
        WorkflowConfig enhancedConfig = injectSystemState(config, nodeKey, pluginKey, runtimeContext);

        log.debug("Executing stateful node '{}' of type '{}'", nodeKey, pluginKey);

        // Execute the node
        ExecutionResult result = executorDispatcher.dispatch(pluginNode, enhancedConfig);

        // Post-execution: handle system state based on result
        handleSystemStatePostExecution(result, nodeKey, pluginKey, runtimeContext);

        return result;
    }

    private WorkflowConfig injectSystemState(WorkflowConfig config, String nodeKey, String pluginKey,
                                             RuntimeContext runtimeContext) {
        Map<String, Object> enhancedInput = new HashMap<>(config.input());

        switch (pluginKey) {
            case "core:flow.loop.for", "core:flow.loop.foreach" -> {
                String stateKey = SystemWorkflowStateKeyBuilder.loopState(nodeKey);
                Object existingState = runtimeContext.getSystemState(stateKey);
                if (existingState != null) {
                    enhancedInput.put("__system_state__", existingState);
                    log.debug("Injected loop state for node '{}'", nodeKey);
                }
            }
            case "core:flow.loop.while" -> {
                String stateKey = SystemWorkflowStateKeyBuilder.whileLoopState(nodeKey);
                Object existingState = runtimeContext.getSystemState(stateKey);
                if (existingState != null) {
                    enhancedInput.put("__system_state__", existingState);
                    log.debug("Injected while loop state for node '{}'", nodeKey);
                }
            }
            case "core:flow.condition.retry" -> {
                String retryKey = SystemWorkflowStateKeyBuilder.retryState(nodeKey);
                Object retryState = runtimeContext.getSystemState(retryKey);
                if (retryState != null) {
                    enhancedInput.put("__retry_state__", retryState);
                    log.debug("Injected retry state for node '{}'", nodeKey);
                }
            }
        }

        return new WorkflowConfig(enhancedInput, config.output());
    }

    private void handleSystemStatePostExecution(ExecutionResult result, String nodeKey, String pluginKey,
                                                RuntimeContext runtimeContext) {
        if (result.getOutput() == null) return;

        switch (pluginKey) {
            case "core:flow.loop.for", "core:flow.loop.foreach" -> {
                String stateKey = SystemWorkflowStateKeyBuilder.loopState(nodeKey);
                Object newState = result.getOutput().get("__system_state__");

                if (newState != null) {
                    runtimeContext.putSystemState(stateKey, newState);
                    log.debug("Updated loop state for node '{}'", nodeKey);
                } else if (result.getStatus() == ExecutionStatus.SUCCESS) {
                    // Loop completed, clean up state
                    runtimeContext.removeSystemState(stateKey);
                    log.debug("Cleaned up completed loop state for node '{}'", nodeKey);
                }
            }
            case "core:flow.loop.while" -> {
                String stateKey = SystemWorkflowStateKeyBuilder.whileLoopState(nodeKey);
                Object newState = result.getOutput().get("__system_state__");

                if (newState != null) {
                    runtimeContext.putSystemState(stateKey, newState);
                    log.debug("Updated while loop state for node '{}'", nodeKey);
                } else if (result.getStatus() == ExecutionStatus.SUCCESS) {
                    runtimeContext.removeSystemState(stateKey);
                    log.debug("Cleaned up completed while loop state for node '{}'", nodeKey);
                }
            }
            case "core:flow.condition.retry" -> {
                String retryKey = SystemWorkflowStateKeyBuilder.retryState(nodeKey);
                Object retryState = result.getOutput().get("__retry_state__");

                if (retryState != null) {
                    runtimeContext.putSystemState(retryKey, retryState);
                    log.debug("Updated retry state for node '{}'", nodeKey);
                }
            }
        }
    }
}
