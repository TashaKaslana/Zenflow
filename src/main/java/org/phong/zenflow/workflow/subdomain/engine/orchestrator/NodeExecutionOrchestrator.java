package org.phong.zenflow.workflow.subdomain.engine.orchestrator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.worker.gateway.ExecutionGateway;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates single node execution with proper logging context.
 * Can be called from workflow engine or from within other executors (like AI nodes calling context variable nodes).
 */
@Component
@AllArgsConstructor
@Slf4j
public class NodeExecutionOrchestrator {
    private final RuntimeContextManager contextManager;
    private final ExecutionGateway executionGateway;
    private final PluginNodeExecutorRegistry pluginNodeRegistry;
    
    /**
     * Execute a node with hierarchical logging context.
     * Supports nested execution: ParentNode -> ChildNode
     */
    public ExecutionResult executeNode(BaseWorkflowNode node,
                                       WorkflowConfig config,
                                       ExecutionContext execCtx) {
        return LogContextManager.withComponent(node.getKey(), () -> {
            LogContext ctx = LogContextManager.snapshot();
            log.info("[traceId={}] [hierarchy={}] Node started", ctx.traceId(), ctx.hierarchy());
            
            execCtx.setCurrentConfig(config);
            
            String parentNodeKey = execCtx.getNodeKey();
            try {
                execCtx.setNodeKey(node.getKey());

                String executorType = node.getPluginNode().getExecutorType();
                if (executorType == null) {
                    throw new WorkflowEngineException("Executor type is not defined for node: " + node.getKey());
                } else if (node.getPluginNode().getNodeId() == null) {
                    throw new WorkflowEngineException("Plugin node ID is not defined for node: " + node.getKey());
                }

                ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                        .taskId(execCtx.taskId())
                        .executorIdentifier(node.getPluginNode().getNodeId().toString())
                        .executorType(executorType)
                        .config(config)
                        .context(execCtx)
                        .pluginNodeId(node.getPluginNode().getNodeId())
                        .build();

                ExecutionResult result = executionGateway.executeAsync(envelope).join();
                
                log.info("[traceId={}] [hierarchy={}] Node finished with status: {}", 
                        ctx.traceId(), ctx.hierarchy(), result.getStatus());
                
                return result;
            } finally {
                execCtx.setNodeKey(parentNodeKey);
            }
        });
    }
    
    /**
     * Execute a synthetic/virtual node by composite key (e.g., "core:context_variable:1.0.0").
     * Used for internal nodes like context variable operations called from executors.
     */
    public ExecutionResult executeSyntheticNodeByKey(String compositeKey,
                                                      String displayName,
                                                      WorkflowConfig config,
                                                      ExecutionContext execCtx) {
        return LogContextManager.withComponent(displayName, () -> {
            LogContext ctx = LogContextManager.snapshot();
            log.info("[traceId={}] [hierarchy={}] Synthetic node started", ctx.traceId(), ctx.hierarchy());
            
            execCtx.setCurrentConfig(config);
            
            // Look up UUID by composite key
            UUID pluginNodeId = pluginNodeRegistry.getIdByCompositeKey(compositeKey)
                    .map(UUID::fromString)
                    .orElseThrow(() -> new WorkflowEngineException(
                            "Plugin node not found for composite key: " + compositeKey));

            ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                    .taskId(execCtx.taskId())
                    .executorIdentifier(pluginNodeId.toString())
                    .executorType("builtin")
                    .config(config)
                    .context(execCtx)
                    .pluginNodeId(pluginNodeId)
                    .build();

            RuntimeContext currentContext = contextManager.getOrCreate(execCtx.getWorkflowRunId().toString());
            Map<String, Object> pendingWrites = currentContext.getPendingWrites();

            ExecutionResult result = executionGateway.executeAsync(envelope).join();
            
            log.info("[traceId={}] [hierarchy={}] Synthetic node finished with status: {}", 
                    ctx.traceId(), ctx.hierarchy(), result.getStatus());

            if (!ExecutionStatus.isSuccessful(result.getStatus())) {
                currentContext.clearPendingWritesWithExclusion(pendingWrites);
            }
            
            return result;
        });
    }
}
