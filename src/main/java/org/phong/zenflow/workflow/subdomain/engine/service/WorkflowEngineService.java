package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextImpl;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextKey;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.event.NodeCommitEvent;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_execution.service.NodeExecutionService;
import org.phong.zenflow.workflow.subdomain.worker.gateway.ExecutionGateway;
import org.phong.zenflow.workflow.subdomain.worker.model.ExecutionTaskEnvelope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final NodeExecutionService nodeExecutionService;
    private final ExecutionGateway executionGateway;
    private final WorkflowNavigatorService workflowNavigatorService;
    private final ApplicationEventPublisher publisher;
    private final RuntimeContextManager contextManager;
    private final TemplateService templateService;
    private final AuthService authService;
    private final ContextValueResolver contextValueResolver;

    @Transactional
    public WorkflowExecutionStatus runWorkflow(Workflow workflow,
                                               UUID workflowRunId,
                                               String startFromNodeKey,
                                               RuntimeContext context) {
        try {
            WorkflowDefinition definition = workflow.getDefinition();
            if (definition == null || definition.nodes() == null) {
                throw new WorkflowEngineException("Workflow definition or nodes are missing for workflow ID: " + workflow.getId());
            }
            WorkflowNodes workflowNodes = definition.nodes();

            if (startFromNodeKey == null) {
                throw new WorkflowEngineException("Start node key is required");
            }
            BaseWorkflowNode workingNode = workflowNodes.findByInstanceKey(startFromNodeKey);
            UUID userIdFromContext = authService.getUserIdFromContext();

            NodeLogPublisher logPublisher = NodeLogPublisher.builder()
                    .publisher(publisher)
                    .workflowId(workflow.getId())
                    .runId(workflowRunId)
                    .userId(userIdFromContext)
                    .build();

            Map<String, WorkflowConfig> nodeConfigs = new HashMap<>(workflowNodes.getAllNodeConfigs());

            ExecutionContext execCtx = ExecutionContextImpl.builder()
                    .workflowId(workflow.getId())
                    .workflowRunId(workflowRunId)
                    .traceId(LogContextManager.snapshot().traceId())
                    .userId(userIdFromContext)
                    .contextManager(contextManager)
                    .logPublisher(logPublisher)
                    .templateService(templateService)
                    .contextValueResolver(contextValueResolver)
                    .nodeConfigs(nodeConfigs)
                    .build();

            return getWorkflowExecutionStatus(workflow.getId(), workflowRunId, context, workingNode, workflowNodes, execCtx);
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflow.getId(), e);
            throw new WorkflowEngineException("Workflow failed", e);
        }
    }

    private WorkflowExecutionStatus getWorkflowExecutionStatus(UUID workflowId,
                                                               UUID workflowRunId,
                                                               RuntimeContext context,
                                                               BaseWorkflowNode workingNode,
                                                               WorkflowNodes workflowNodes,
                                                               ExecutionContext execCtx) {
        WorkflowExecutionStatus executionStatus = WorkflowExecutionStatus.COMPLETED;
        ExecutionResult result;

        while (workingNode != null) {
            result = setupAndExecutionWorkflow(workflowId, workflowRunId, context, workingNode, execCtx);
            WorkflowNavigatorService.ExecutionStepOutcome outcome = workflowNavigatorService.handleExecutionResult(workflowId, workflowRunId, workingNode, result, workflowNodes, context);
            workingNode = outcome.nextNode();
            executionStatus = outcome.status();
        }

        return executionStatus;
    }

    private ExecutionResult setupAndExecutionWorkflow(UUID workflowId,
                                                      UUID workflowRunId,
                                                      RuntimeContext context,
                                                      BaseWorkflowNode workingNode,
                                                      ExecutionContext execCtx) {
        ExecutionResult result;
        nodeExecutionService.startNode(workflowRunId, workingNode.getKey());

        execCtx.setNodeKey(workingNode.getKey());
        execCtx.setPluginNodeId(workingNode.getPluginNode().getNodeId());
        WorkflowConfig config = workingNode.getConfig() != null ? workingNode.getConfig() : new WorkflowConfig();
        WorkflowConfig resolvedConfig = execCtx.resolveConfig(workingNode.getKey(), config);

        result = executeWorkingNode(workingNode, resolvedConfig, execCtx);

        Map<String, Object> output = result.getOutput();
        if (output != null) {
            context.processOutputWithMetadata(String.format("%s.output", workingNode.getKey()), output);
        } else {
            log.warn("Output of node {} is null, skipping putting into context", workingNode.getKey());
        }

        Object callbackUrl = contextManager.getOrCreate(workflowRunId.toString())
                .get(ExecutionContextKey.CALLBACK_URL.key());
        nodeExecutionService.resolveNodeExecution(
                workflowId,
                workflowRunId,
                workingNode,
                result,
                callbackUrl != null ? callbackUrl.toString() : null
        );

        if (result.getStatus() == ExecutionStatus.COMMIT) {
            publisher.publishEvent(new NodeCommitEvent(workflowId, workflowRunId, workingNode.getKey()));
        }

        return result;
    }

    private ExecutionResult executeWorkingNode(BaseWorkflowNode workingNode,
                                               WorkflowConfig resolvedConfig,
                                               ExecutionContext execCtx) {
        return LogContextManager.withComponent(workingNode.getKey(), () -> {
            LogContext ctx = LogContextManager.snapshot();
            log.info("[traceId={}] [hierarchy={}] Node started", ctx.traceId(), ctx.hierarchy());
            execCtx.setCurrentConfig(resolvedConfig);
            execCtx.setNodeKey(workingNode.getKey());

            String executorType = workingNode.getPluginNode().getExecutorType();
            if (executorType == null) {
                throw new WorkflowEngineException("Executor type is not defined for node: " + workingNode.getKey());
            } else if (workingNode.getPluginNode().getNodeId() == null) {
                throw new WorkflowEngineException("Plugin node ID is not defined for node: " + workingNode.getKey());
            }

            ExecutionTaskEnvelope envelope = ExecutionTaskEnvelope.builder()
                    .taskId(execCtx.taskId())
                    .executorIdentifier(workingNode.getPluginNode().getNodeId().toString())
                    .executorType(executorType)
                    .config(resolvedConfig)
                    .context(execCtx)
                    .pluginNodeId(workingNode.getPluginNode().getNodeId())
                    .build();

            ExecutionResult result = executionGateway.executeAsync(envelope).join();
            log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
            return result;
        });
    }
}
