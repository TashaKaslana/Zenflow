package org.phong.zenflow.workflow.subdomain.runner.listener;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@AllArgsConstructor
public class WorkflowRunnerListener {
    private WorkflowRunnerService workflowRunnerService;

    @TransactionalEventListener
    @Async("applicationTaskExecutor")
    public void onWorkflowRunEvent(WorkflowRunnerPublishableEvent event) {
            workflowRunnerService.runWorkflow(
                    event.getWorkflowRunId(),
                    event.getTriggerType(),
                    event.getTriggerExecutorId(),
                    event.getWorkflowId(),
                    event.request()
            );
    }
}
