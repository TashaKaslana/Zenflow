package org.phong.zenflow.workflow.subdomain.worker.node_gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowFinished;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeFinished;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeStart;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowStartEvent;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.services.WorkerExecutionService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionGateway {
    private final WorkerExecutionService workerExecutionService;
    private final WorkflowRunnerService workflowRunnerService;
    private final WorkflowEngineService workflowEngineService;

    @TransactionalEventListener(fallbackExecution = true)
    @Async("applicationTaskExecutor")
    public void onWorkflowRunEvent(WorkflowStartEvent event) {
        log.debug("Publishing WorkflowRunnerPublishableEvent for workflow {}, run {}, triggerType {}, triggerExecutorId {}, payload {}",
                event.getWorkflowId(),
                event.getWorkflowRunId(),
                event.getTriggerType(),
                event.getTriggerExecutorId(),
                event.request()
        );
        workflowRunnerService.runWorkflow(
                event.getWorkflowRunId(),
                event.getTriggerType(),
                event.getTriggerExecutorId(),
                event.getWorkflowId(),
                event.request()
        );
    }

    @EventListener()
    private void handleWorkflowNodeStart(WorkflowNodeStart workflowNodeStart) {
        workerExecutionService.startWorkflowNode(workflowNodeStart);
    }

    @EventListener()
    private void handleWorkflowNodeFinished(WorkflowNodeFinished event) {
        workflowEngineService.processWorkflow(event);
    }

    @EventListener()
    private void handleWorkflowFinished(WorkflowFinished event) {
        workflowEngineService.processWorkflowFinishedState(event);
    }
}
