package org.phong.zenflow.workflow.subdomain.runner.listener;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.runner.event.WorkflowRunnerPublishableEvent;
import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@AllArgsConstructor
public class WorkflowRunnerListener {
    private WorkflowRunnerService workflowRunnerService;

    @EventListener
    @Async("applicationTaskExecutor")
    public void onWorkflowRunEvent(WorkflowRunnerPublishableEvent event) {
        if (shouldRunAfterCommit(event.getTriggerType())) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        workflowRunnerService.runWorkflow(
                                event.getWorkflowRunId(),
                                event.getTriggerType(),
                                event.getWorkflowId(),
                                event.request()
                        );
                    }
                });
            } else {
                // No active transaction — run immediately
                workflowRunnerService.runWorkflow(
                        event.getWorkflowRunId(),
                        event.getTriggerType(),
                        event.getWorkflowId(),
                        event.request()
                );
            }
        } else {
            // Run immediately
            workflowRunnerService.runWorkflow(
                    event.getWorkflowRunId(),
                    event.getTriggerType(),
                    event.getWorkflowId(),
                    event.request()
            );
        }
    }

    private boolean shouldRunAfterCommit(TriggerType triggerType) {
        return switch (triggerType) {
            case SCHEDULE, MANUAL -> true;
            case WEBHOOK, EVENT -> false;
        };
    }
}
