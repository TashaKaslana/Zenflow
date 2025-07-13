package org.phong.zenflow.workflow.subdomain.engine.dto;

import java.util.UUID;

public record WorkFlowEngineResponse(UUID workflowRunId, String statusUrl) {
}
