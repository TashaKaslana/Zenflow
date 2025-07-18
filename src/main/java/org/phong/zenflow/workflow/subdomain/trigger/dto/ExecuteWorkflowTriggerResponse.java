package org.phong.zenflow.workflow.subdomain.trigger.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ExecuteWorkflowTriggerResponse(UUID workflowRunId, @NotNull String statusUrl) {
}
