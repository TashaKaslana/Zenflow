package org.phong.zenflow.workflow.subdomain.engine.listener;

import lombok.AllArgsConstructor;
import org.phong.zenflow.workflow.subdomain.engine.event.WorkflowEngineEvent;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowEngineService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@AllArgsConstructor
public class WorkflowEngineListener {
    private final WorkflowEngineService workflowEngineService;

    @TransactionalEventListener
    @Async
    public void handleWorkflowEngine(WorkflowEngineEvent event) {
        workflowEngineService.runWorkflow(event.workflowId(), event.workflowRunId(), event.fromNodeKey());
    }
}
