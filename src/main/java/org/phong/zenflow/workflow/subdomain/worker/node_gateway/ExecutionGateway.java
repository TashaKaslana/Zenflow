package org.phong.zenflow.workflow.subdomain.worker.node_gateway;

import org.phong.zenflow.workflow.subdomain.worker.node_gateway.event.WorkflowStart;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ExecutionGateway {

    @EventListener()
    private void handleWorkflowStart(WorkflowStart event) {

    }
}
