package org.phong.zenflow.workflow.subdomain.worker.node_gateway.services;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.enums.WorkflowExecutionStatus;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeFinished;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeStart;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkerExecutionService {
    private final RuntimeContextManager runtimeContextManager;
    private final TemplateService templateService;
    private final ContextValueResolver contextValueResolver;
    private final NodeExecutionOrchestrator nodeExecutionOrchestrator;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkflowService workflowService;

    public void startWorkflowNode(WorkflowNodeStart task) {
        RuntimeContext runtimeContext = runtimeContextManager.getOrCreate(task.generateTaskId());

        String userId = runtimeContext.get("__workflow__userId").toString();
        String workflowId = runtimeContext.get("__workflow__workflowId").toString();
        Workflow workflow = workflowService.getWorkflow(UUID.fromString(workflowId));
        Map<String, WorkflowConfig> allNodeConfigs = workflow.getDefinition().nodes().getAllNodeConfigs();
        BaseWorkflowNode workflowNode = workflow.getDefinition().nodes()
                .findByInstanceKey(task.getInstanceNodeKey());
        WorkflowConfig currentConfig = workflowNode.getConfig();

        NodeLogPublisher nodeLogPublisher = NodeLogPublisher.builder()
                .workflowId(UUID.fromString(workflowId))
                .runId(task.getWorkflowRunId())
                .userId(UUID.fromString(userId))
                .publisher(eventPublisher)
                .build();

        ExecutionContext executionContext = ExecutionContextImpl.builder()
                .workflowRunId(task.getWorkflowRunId())
                .nodeConfigs(allNodeConfigs)
                .currentConfig(currentConfig)
                .runtimeContext(runtimeContext)
                .templateService(templateService)
                .contextValueResolver(contextValueResolver)
                .orchestrator(nodeExecutionOrchestrator)
                .logPublisher(nodeLogPublisher)
                .build();

        //switch to WorkflowExecutionStatus in worker
        ExecutionResult executionResult = nodeExecutionOrchestrator.executeNode(workflowNode, executionContext);

        eventPublisher.publishEvent(
                new WorkflowNodeFinished(task.getWorkflowRunId(),
                        WorkflowExecutionStatus.mapStatus(executionResult.getStatus()),
                        executionResult,
                        null
                )
        );
    }
}
