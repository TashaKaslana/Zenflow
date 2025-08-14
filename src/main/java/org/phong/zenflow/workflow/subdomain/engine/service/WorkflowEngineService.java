package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.dto.RuntimeMetadata;
import org.phong.zenflow.plugin.subdomain.execution.services.PluginNodeExecutorDispatcher;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.event.NodeCommitEvent;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.service.NodeLogService;
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
    private final NodeLogService nodeLogService;
    private final WorkflowValidationService workflowValidationService;
    private final PluginNodeExecutorDispatcher executorDispatcher;
    private final WorkflowNavigatorService workflowNavigatorService;
    private final ApplicationEventPublisher publisher;
    

    @Transactional
    public WorkflowExecutionStatus runWorkflow(UUID workflowId,
                                               UUID workflowRunId,
                                               @Nullable String startFromNodeKey) {
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

            return getWorkflowExecutionStatus(workflowId, workflowRunId, workingNode, workflowNodes);
        } catch (Exception e) {
            log.warn("Error running workflow with ID: {}", workflowId, e);
            throw new WorkflowEngineException("Workflow failed", e);
        }
    }

    private WorkflowExecutionStatus getWorkflowExecutionStatus(UUID workflowId,
                                                               UUID workflowRunId,
                                                               BaseWorkflowNode workingNode,
                                                               List<BaseWorkflowNode> workflowNodes) {
        WorkflowExecutionStatus executionStatus = WorkflowExecutionStatus.COMPLETED;
        ExecutionResult result;

        while (workingNode != null) {
            result = setupAndExecutionWorkflow(workflowId, workflowRunId, workingNode);
            WorkflowNavigatorService.ExecutionStepOutcome outcome = workflowNavigatorService.handleExecutionResult(workflowId, workflowRunId, workingNode, result, workflowNodes);
            workingNode = outcome.nextNode();
            executionStatus = outcome.status();
        }

        return executionStatus;
    }


    private ExecutionResult setupAndExecutionWorkflow(UUID workflowId,
                                                      UUID workflowRunId,
                                                      BaseWorkflowNode workingNode) {
        ExecutionResult result;
        nodeLogService.startNode(workflowRunId, workingNode.getKey());

        RuntimeContext context = RuntimeContextPool.getContext(workflowRunId);

        WorkflowConfig config = workingNode.getConfig() != null ? workingNode.getConfig() : new WorkflowConfig();
        WorkflowConfig resolvedConfig = context.resolveConfig(workingNode.getKey(), config);

        result = executeWorkingNode(workingNode, resolvedConfig, workflowRunId);

        Map<String, Object> output = result.getOutput();
        if (output != null) {
            context.processOutputWithMetadata(String.format("%s.output", workingNode.getKey()), output);
        } else {
            log.warn("Output of node {} is null, skipping putting into context", workingNode.getKey());
        }
        nodeLogService.resolveNodeLog(workflowId, workflowRunId, workingNode, result);

        if (result.getStatus() == org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus.COMMIT) {
            publisher.publishEvent(new NodeCommitEvent(workflowId, workflowRunId, workingNode.getKey()));
        }

        return result;
    }

    private ExecutionResult executeWorkingNode(BaseWorkflowNode workingNode,
                                               WorkflowConfig resolvedConfig,
                                               UUID workflowRunId) {
        ExecutionResult result;

        ValidationResult validationResult = workflowValidationService.validateRuntime(
                workingNode.getKey(),
                resolvedConfig,
                workingNode.getPluginNode().toCacheKey(),
                workflowRunId
        );
        if (!validationResult.isValid()) {
            return ExecutionResult.validationError(validationResult, workingNode.getKey());
        }

        ExecutionInput input = new ExecutionInput(resolvedConfig, new RuntimeMetadata(workflowRunId));
        result = executorDispatcher.dispatch(workingNode.getPluginNode(), input);

        return result;
    }
}
