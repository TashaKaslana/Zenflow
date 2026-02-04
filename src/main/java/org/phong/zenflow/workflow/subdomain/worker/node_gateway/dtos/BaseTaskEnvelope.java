package org.phong.zenflow.workflow.subdomain.worker.node_gateway.dtos;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class BaseTaskEnvelope {
    private final String taskId;
    private final UUID workflowRunId;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public BaseTaskEnvelope(UUID workflowRunId) {
        taskId = generateTaskId();
        this.workflowRunId = workflowRunId;
    }

    public String generateTaskId() {
        if (workflowRunId == null) {
            throw new IllegalArgumentException("Some how workflowRunId is null. It must be not null");
        }

        return workflowRunId + "@" + createdAt;
    }
}
