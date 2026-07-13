package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.worker.node_gateway.dtos.BaseTaskEnvelope;

import java.util.UUID;

@Getter
public class WorkflowNodeStart extends BaseTaskEnvelope {
    public WorkflowNodeStart(UUID workflowRunId, String instanceNodeKey) {
        super(workflowRunId);
        this.instanceNodeKey = instanceNodeKey;
    }

    private final String instanceNodeKey;
}
