package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextImpl;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.worker.gateway.ExecutionGateway;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for executing a single plugin node outside a workflow context.
 * This mimics the execution behavior of a node within a workflow by
 * performing runtime validation and dispatching through the standard
 * execution gateway.
 */
@Service
@AllArgsConstructor
@Slf4j
public class SingleNodeExecutionService {

    private final ExecutionGateway executionGateway;
    private final RuntimeContextManager contextManager;
    private final ApplicationEventPublisher publisher;
    private final TemplateService templateService;

    /**
     * Execute a plugin node with the provided configuration.
     *
     * @param pluginNode the plugin node to execute
     * @param node the instance node
     * @return the {@link ExecutionResult} returned by the node's executor
     */
    public ExecutionResult executeNode(PluginNode pluginNode, BaseWorkflowNode node) {
        RuntimeContext context = new RuntimeContext();
        context.initialize(Map.of(), Map.of(), Map.of());
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        contextManager.assign(runId.toString(), context);
        NodeLogPublisher logPublisher = NodeLogPublisher.builder()
                .publisher(publisher)
                .workflowId(workflowId)
                .runId(runId)
                .userId(null)
                .build();

        ExecutionContext execCtx = ExecutionContextImpl.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId(LogContextManager.snapshot().traceId())
                .userId(null)
                .contextManager(contextManager)
                .logPublisher(logPublisher)
                .templateService(templateService)
                .build();

        execCtx.setNodeKey(node.getKey());
        execCtx.setPluginNodeId(pluginNode.getId());
        execCtx.setExecutorType(pluginNode.getExecutorType());

        WorkflowConfig config = node.getConfig();
        WorkflowConfig safeConfig = (config != null) ? config : new WorkflowConfig();
        WorkflowConfig resolvedConfig = execCtx.resolveConfig(node.getKey(), safeConfig);
        execCtx.setCurrentConfig(resolvedConfig);

        return LogContextManager.withComponent(node.getKey(), () -> {
            LogContext ctx = LogContextManager.snapshot();
            log.info("[traceId={}] [hierarchy={}] Node started", ctx.traceId(), ctx.hierarchy());
            try {
                ExecutionResult result = executionGateway.executeAsync(execCtx).get();
                log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Execution interrupted for node: " + node.getKey(), e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException("Execution failed for node: " + node.getKey(), cause);
            }
        });
    }
}
