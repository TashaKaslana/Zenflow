package org.phong.zenflow.workflow.subdomain.node_logs.logging;

import lombok.Getter;
import org.phong.zenflow.workflow.subdomain.node_logs.service.NodeLogPublisher;

import java.util.UUID;

@Getter
public class ExecutionContext {
    private final UUID workflowId;
    private final UUID workflowRunId;
    private final String nodeKey;
    private final NodeLogPublisher log;

    public ExecutionContext(UUID workflowId, UUID workflowRunId, String nodeKey) {
        this.workflowId = workflowId;
        this.workflowRunId = workflowRunId;
        this.nodeKey = nodeKey;
        this.log = NodeLogPublisher.builder()
                .workflowId(workflowId)
                .runId(workflowRunId)
                .nodeKey(nodeKey)
                .build();
    }

    public ExecutionContext(UUID workflowId, UUID workflowRunId, String nodeKey, UUID userId) {
        this.workflowId = workflowId;
        this.workflowRunId = workflowRunId;
        this.nodeKey = nodeKey;
        this.log = NodeLogPublisher.builder()
                .workflowId(workflowId)
                .runId(workflowRunId)
                .nodeKey(nodeKey)
                .userId(userId)
                .build();
    }
}
