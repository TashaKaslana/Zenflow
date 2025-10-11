package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
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
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class SingleNodeExecutionService {

    private final ExecutionGateway executionGateway;
    private final RuntimeContextManager contextManager;
    private final ApplicationEventPublisher publisher;
    private final TemplateService templateService;
    private final AuthService authService;

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
                .userId(authService.getUserIdFromContext())
                .build();

        ExecutionContext execCtx = ExecutionContextImpl.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId(LogContextManager.snapshot().traceId())
                .userId(authService.getUserIdFromContext())
                .contextManager(contextManager)
                .logPublisher(logPublisher)
                .templateService(templateService)
                .build();

        execCtx.setNodeKey(node.getKey());
        execCtx.setPluginNodeId(pluginNode.getId());

        WorkflowConfig config = node.getConfig();
        WorkflowConfig safeConfig = (config != null) ? config : new WorkflowConfig();
        WorkflowConfig resolvedConfig = execCtx.resolveConfig(node.getKey(), safeConfig);
        execCtx.setCurrentConfig(resolvedConfig);

        ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                .taskId(execCtx.taskId())
                .executorIdentifier(pluginNode.getId().toString())
                .executorType(pluginNode.getExecutorType())
                .config(resolvedConfig)
                .context(execCtx)
                .pluginNodeId(pluginNode.getId())
                .build();

        return LogContextManager.withComponent(node.getKey(), () -> {
            LogContext ctx = LogContextManager.snapshot();
            log.info("[traceId={}] [hierarchy={}] Node started", ctx.traceId(), ctx.hierarchy());

            CompletableFuture<ExecutionResult> future = executionGateway.executeAsync(envelope);
            ExecutionResult result = future.join();

            if (result.getErrorType() == ExecutionError.INTERRUPTED) {
                log.warn("Execution interrupted for node: {}", node.getKey());
            }

            log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
            return result;
        });
    }
}
