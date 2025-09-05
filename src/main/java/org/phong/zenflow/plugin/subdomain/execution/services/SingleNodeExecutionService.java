package org.phong.zenflow.plugin.subdomain.execution.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import java.util.UUID;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

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
    private final RuntimeContextManager contextManager;
    private final ApplicationEventPublisher publisher;
    private final TemplateService templateService;

    /**
     * Execute a pluginNode node with the provided configuration.
     *
     * @param pluginNode the pluginNode node to execute
     * @param node the instance node
     * @return the {@link ExecutionResult} returned by the node's executor
     */
    public ExecutionResult executeNode(PluginNode pluginNode, BaseWorkflowNode node) {
        // Initialize runtime context similar to workflow runs
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

        ExecutionContext execCtx = ExecutionContext.builder()
                .workflowId(workflowId)
                .workflowRunId(runId)
                .traceId(LogContextManager.snapshot().traceId())
                .userId(null)
                .contextManager(contextManager)
                .logPublisher(logPublisher)
                .templateService(templateService)
                .build();

        execCtx.setNodeKey(pluginNode.getCompositeKey());
        WorkflowConfig config = node.getConfig();
        WorkflowConfig safeConfig = (config != null) ? config : new WorkflowConfig();
        WorkflowConfig resolvedConfig = execCtx.resolveConfig(pluginNode.getCompositeKey(), safeConfig);

        return LogContextManager.withComponent(pluginNode.getCompositeKey(), () -> {
            LogContext ctx = LogContextManager.snapshot();
            log.info("[traceId={}] [hierarchy={}] Node started", ctx.traceId(), ctx.hierarchy());
            execCtx.setNodeKey(pluginNode.getCompositeKey());

            ValidationResult validationResult = workflowValidationService.validateRuntime(
                    node.getKey(),
                    resolvedConfig,
                    pluginNode.getId().toString(),
                    execCtx
            );
            if (!validationResult.isValid()) {
                log.warn("Validation failed for node {}: {}", pluginNode.getCompositeKey(), validationResult.getErrors());
                log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
                return ExecutionResult.validationError(validationResult, pluginNode.getCompositeKey());
            }
            ExecutionResult result = executorDispatcher.dispatch(
                    pluginNode.getId().toString(),
                    pluginNode.getExecutorType(),
                    resolvedConfig,
                    execCtx
            );
            log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
            return result;
        });
    }
}

