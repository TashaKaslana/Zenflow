package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.event.NodeCommitEvent;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_execution.service.NodeExecutionService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class WorkflowEngineService {
    private final WorkflowRepository workflowRepository;
    private final NodeExecutionService nodeExecutionService;
    private final WorkflowValidationService workflowValidationService;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final WorkflowNavigatorService workflowNavigatorService;
    private final ApplicationEventPublisher publisher;
    private final RuntimeContextManager contextManager;
    

    @Transactional
    public WorkflowExecutionStatus runWorkflow(UUID workflowId,
                                               UUID workflowRunId,
                                               @Nullable String startFromNodeKey,
                                               RuntimeContext context) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId).orElseThrow(
                    () -> new WorkflowEngineException("Workflow not found with ID: " + workflowId)
            );

            WorkflowDefinition definition = workflow.getDefinition();
            if (definition == null || definition.nodes() == null) {
                throw new WorkflowEngineException("Workflow definition or nodes are missing for workflow ID: " + workflowId);
            }
            List<BaseWorkflowNode> workflowNodes = definition.nodes();

            String currentNodeKey = (startFromNodeKey != null) ? startFromNodeKey : workflow.getStartNode();
            BaseWorkflowNode workingNode = workflowNavigatorService.findNodeByKey(workflowNodes, currentNodeKey);

            NodeLogPublisher logPublisher = NodeLogPublisher.builder()
                    .publisher(publisher)
                    .workflowId(workflowId)
                    .runId(workflowRunId)
                    .userId(null)
                    .build();

            ExecutionContext execCtx = ExecutionContext.builder()
                    .workflowId(workflowId)
                    .workflowRunId(workflowRunId)
                    .traceId(LogContextManager.snapshot().traceId())
                    .userId(null)
                    .contextManager(contextManager)
                    .logPublisher(logPublisher)
                    .build();

            return getWorkflowExecutionStatus(workflowId, workflowRunId, context, workingNode, workflowNodes, execCtx);
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            throw new WorkflowEngineException("Workflow failed", e);
        }
    }

    private WorkflowExecutionStatus getWorkflowExecutionStatus(UUID workflowId,
                                                               UUID workflowRunId,
                                                               RuntimeContext context,
                                                               BaseWorkflowNode workingNode,
                                                               List<BaseWorkflowNode> workflowNodes,
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

        WorkflowConfig config = workingNode.getConfig() != null ? workingNode.getConfig() : new WorkflowConfig();
        WorkflowConfig resolvedConfig = context.resolveConfig(workingNode.getKey(), config);

        result = executeWorkingNode(workingNode, resolvedConfig, execCtx);

        Map<String, Object> output = result.getOutput();
        if (output != null) {
            context.processOutputWithMetadata(String.format("%s.output", workingNode.getKey()), output);
        } else {
            log.warn("Output of node {} is null, skipping putting into context", workingNode.getKey());
        }
        nodeExecutionService.resolveNodeExecution(workflowId, workflowRunId, workingNode, result);

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

            ValidationResult validationResult = workflowValidationService.validateRuntime(
                    workingNode.getKey(),
                    resolvedConfig,
                    workingNode.getPluginNode().toCacheKey(),
                    execCtx
            );
            if (!validationResult.isValid()) {
                log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
                return ExecutionResult.validationError(validationResult, workingNode.getKey());
            }

            execCtx.setNodeKey(workingNode.getKey());

            ExecutionResult result = executorDispatcher.dispatch(workingNode.getPluginNode(), resolvedConfig, execCtx);
            log.info("[traceId={}] [hierarchy={}] Node finished", ctx.traceId(), ctx.hierarchy());
            return result;
        });
    }
}
