package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
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
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final NodeExecutionService nodeExecutionService;
    private final WorkflowValidationService workflowValidationService;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final WorkflowNavigatorService workflowNavigatorService;
    private final ApplicationEventPublisher publisher;
    private final RuntimeContextManager contextManager;
    private final TemplateService templateService;
    

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

            NodeLogPublisher logPublisher = NodeLogPublisher.builder()
                    .publisher(publisher)
                    .workflowId(workflow.getId())
                    .runId(workflowRunId)
                    .userId(null)
                    .build();

            ExecutionContext execCtx = ExecutionContext.builder()
                    .workflowId(workflow.getId())
                    .workflowRunId(workflowRunId)
                    .traceId(LogContextManager.snapshot().traceId())
                    .userId(null)
                    .contextManager(contextManager)
                    .logPublisher(logPublisher)
                    .templateService(templateService)
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
        WorkflowConfig config = workingNode.getConfig() != null ? workingNode.getConfig() : new WorkflowConfig();
        WorkflowConfig resolvedConfig = execCtx.resolveConfig(workingNode.getKey(), config);

        result = executeWorkingNode(workingNode, resolvedConfig, execCtx);

        Map<String, Object> output = result.getOutput();
        if (output != null) {
            context.processOutputWithMetadata(String.format("%s.output", workingNode.getKey()), output);
        } else {
            log.warn("Output of node {} is null, skipping putting into context", workingNode.getKey());
        }
        nodeExecutionService.resolveNodeExecution(
                workflowId,
                workflowRunId,
                workingNode,
                result,
                execCtx.read(ExecutionContextKey.CALLBACK_URL, String.class)
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

            // Use UUID if available, fallback to a composite key
            String templateString = workingNode.getPluginNode().getNodeId() != null ?
                workingNode.getPluginNode().getNodeId().toString() :
                workingNode.getPluginNode().toCacheKey();

            ValidationResult validationResult = workflowValidationService.validateRuntime(
                    workingNode.getKey(),
                    resolvedConfig,
                    templateString,
                    execCtx
            );
            if (!validationResult.isValid()) {
                log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
                return ExecutionResult.validationError(validationResult, workingNode.getKey());
            }

            execCtx.setNodeKey(workingNode.getKey());

            // Use UUID for dispatcher if available, fallback to a composite key
            String executorKey = workingNode.getPluginNode().getNodeId() != null ?
                workingNode.getPluginNode().getNodeId().toString() :
                workingNode.getPluginNode().toCacheKey();

            String executorType = workingNode.getPluginNode().getExecutorType();
            if (executorType == null) {
                throw new WorkflowEngineException("Executor type is not defined for node: " + workingNode.getKey());
            }

            ExecutionResult result = executorDispatcher.dispatch(
                    executorKey,
                    workingNode.getPluginNode().getExecutorType(),
                    resolvedConfig,
                    execCtx
            );
            log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
            return result;
        });
    }
}
