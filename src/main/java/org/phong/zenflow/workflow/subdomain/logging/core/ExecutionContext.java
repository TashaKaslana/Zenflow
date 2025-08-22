package org.phong.zenflow.workflow.subdomain.logging.core;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Builder
public class ExecutionContext {
    private final UUID workflowId;
    private final UUID workflowRunId;
    @Setter
    private String nodeKey;
    private final UUID userId;
    private final NodeLogPublisher log;

    public static class ExecutionContextBuilder {
        public ExecutionContext build() {
            NodeLogPublisher publisher = NodeLogPublisher.builder()
                    .workflowId(workflowId)
                    .runId(workflowRunId)
                    .nodeKey(nodeKey)
                    .userId(userId)
                    .build();
            return new ExecutionContext(workflowId, workflowRunId, nodeKey, userId, publisher);
        }
    }
}