package org.phong.zenflow.workflow.subdomain.engine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextImpl;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextKey;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogContextManager;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.engine.event.NodeCommitEvent;
import org.phong.zenflow.workflow.subdomain.engine.exception.WorkflowEngineException;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowNodes;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_execution.service.NodeExecutionService;
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
    private final NodeExecutionOrchestrator nodeExecutionOrchestrator;
    private final WorkflowNavigatorService workflowNavigatorService;
    private final ApplicationEventPublisher publisher;
    private final RuntimeContextManager contextManager;
    private final TemplateService templateService;
    private final AuthService authService;
    private final ContextValueResolver contextValueResolver;
    private final PluginNodeExecutorRegistry pluginNodeRegistry;

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
            Map<String, BaseWorkflowNode> nodeDefinitions = new HashMap<>();
            workflowNodes.forEach((key, node) -> nodeDefinitions.put(key, new BaseWorkflowNode(node)));

            ExecutionContext execCtx = ExecutionContextImpl.builder()
                    .workflowId(workflow.getId())
                    .workflowRunId(workflowRunId)
                    .traceId(LogContextManager.snapshot().traceId())
                    .userId(userIdFromContext)
                    .contextManager(contextManager)
                    .logPublisher(logPublisher)
                    .templateService(templateService)
                    .contextValueResolver(contextValueResolver)
                    .orchestrator(nodeExecutionOrchestrator)
                    .pluginNodeRegistry(pluginNodeRegistry)
                    .nodeConfigs(nodeConfigs)
                    .workflowNodes(nodeDefinitions)
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
        Map<String, Object> outputForHistory;

        while (workingNode != null) {
            var executionOutcome = setupAndExecutionWorkflow(workflowId, workflowRunId, context, workingNode, execCtx);
            result = executionOutcome.result;
            outputForHistory = executionOutcome.output;
            
            WorkflowNavigatorService.ExecutionStepOutcome outcome = workflowNavigatorService.handleExecutionResult(
                    workflowId, workflowRunId, workingNode, result, workflowNodes, context, outputForHistory);
            workingNode = outcome.nextNode();
            executionStatus = outcome.status();
        }

        return executionStatus;
    }
    
    private record ExecutionOutcome(ExecutionResult result, Map<String, Object> output) {}

    private ExecutionOutcome setupAndExecutionWorkflow(UUID workflowId,
                                                      UUID workflowRunId,
                                                      RuntimeContext context,
                                                      BaseWorkflowNode workingNode,
                                                      ExecutionContext execCtx) {
        ExecutionResult result;
        nodeExecutionService.startNode(workflowRunId, workingNode.getKey());

        execCtx.setNodeKey(workingNode.getKey());
        execCtx.setPluginNodeId(workingNode.getPluginNode().getNodeId());
        WorkflowConfig config = workingNode.getConfig() != null ? workingNode.getConfig() : new WorkflowConfig();

        result = nodeExecutionOrchestrator.executeNode(workingNode, config, execCtx);

        Map<String, Object> outputForHistory = context.getPendingWrites();
        
        // Flush pending writes if execution succeeded
        if (ExecutionStatus.isSuccessful(result.getStatus())) {
            context.flushPendingWrites(execCtx.getNodeKey());
        } else {
            context.clearPendingWrites();
        }

        Object callbackUrl = contextManager.getOrCreate(workflowRunId.toString())
                .get(ExecutionContextKey.CALLBACK_URL.key());
        nodeExecutionService.resolveNodeExecution(
                workflowId,
                workflowRunId,
                workingNode,
                result,
                outputForHistory,
                callbackUrl != null ? callbackUrl.toString() : null
        );

        if (result.getStatus() == ExecutionStatus.COMMIT) {
            publisher.publishEvent(new NodeCommitEvent(workflowId, workflowRunId, workingNode.getKey()));
        }

        return new ExecutionOutcome(result, outputForHistory);
    }


}
