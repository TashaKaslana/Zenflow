package org.phong.zenflow.workflow.subdomain.workflow_run.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.WorkflowStatus;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun}
 */
public record WorkflowRunDto(@NotNull UUID id, @NotNull UUID workflowId, @NotNull WorkflowStatus status,
                            String error, TriggerType triggerType, @NotNull OffsetDateTime startedAt,
                            OffsetDateTime endedAt) implements Serializable {
}
