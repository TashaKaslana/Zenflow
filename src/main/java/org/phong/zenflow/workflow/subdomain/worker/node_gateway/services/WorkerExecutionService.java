package org.phong.zenflow.workflow.subdomain.worker.node_gateway.services;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextManager;
import org.phong.zenflow.workflow.subdomain.context.resolution.ContextValueResolver;
import org.phong.zenflow.workflow.subdomain.engine.orchestrator.NodeExecutionOrchestrator;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeStart;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkerExecutionService {
    private final RuntimeContextManager runtimeContextManager;
    private final TemplateService templateService;
    private final ContextValueResolver contextValueResolver;
    private final NodeExecutionOrchestrator nodeExecutionOrchestrator;
    private final ApplicationEventPublisher eventPublisher;

    public void executeTask(WorkflowNodeStart task) {
        RuntimeContext runtimeContext = runtimeContextManager.getOrCreate(task.getTaskId());

        String userId = runtimeContext.get("__workflow__userId").toString();
        String workflowId = runtimeContext.get("__workflow__workflowId").toString();

        NodeLogPublisher nodeLogPublisher = NodeLogPublisher.builder()
                .workflowId(UUID.fromString(workflowId))
                .runId(task.getWorkflowRunId())
                .userId(UUID.fromString(userId))
                .publisher(eventPublisher)
                .build();

        ExecutionContext executionContext = ExecutionContextImpl.builder()
                .workflowRunId(task.getWorkflowRunId())
                .runtimeContext(runtimeContext)
                .templateService(templateService)
                .contextValueResolver(contextValueResolver)
                .orchestrator(nodeExecutionOrchestrator)
                .logPublisher(nodeLogPublisher)
                .build();


    }
}
