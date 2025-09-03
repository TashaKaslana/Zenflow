package org.phong.zenflow.workflow.subdomain.runner.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@AllArgsConstructor
@Slf4j
public class WorkflowRunnerListener {
    private WorkflowRunnerService workflowRunnerService;

    @TransactionalEventListener(fallbackExecution = true)
    @Async("applicationTaskExecutor")
    public void onWorkflowRunEvent(WorkflowRunnerPublishableEvent event) {
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

    @TransactionalEventListener
    @Async("applicationTaskExecutor")
    public void onWorkflowRunEventConcrete(WorkflowTriggerEvent event) {
        onWorkflowRunEvent(event);
    }
}
