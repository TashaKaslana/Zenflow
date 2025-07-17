package org.phong.zenflow.workflow.subdomain.engine.event;

import java.util.UUID;

public record WorkflowEngineEvent(UUID workflowId, UUID workflowRunId, String fromNodeKey) {
}
