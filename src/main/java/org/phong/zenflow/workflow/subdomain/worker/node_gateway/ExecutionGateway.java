package org.phong.zenflow.workflow.subdomain.worker.node_gateway;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowNodeStart;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowStart;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.services.WorkerExecutionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutionGateway {
    private final WorkerExecutionService workerExecutionService;

    @EventListener()
    private void handleWorkflowStart(WorkflowStart event) {

    }

    @EventListener()
    private void handleWorkflowNodeStart(WorkflowNodeStart workflowNodeStart) {
        workerExecutionService.startWorkflowNode(workflowNodeStart);
    }
}
