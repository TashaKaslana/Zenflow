package org.phong.zenflow.workflow.subdomain.workflow_run.dto;

import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.WorkflowStatus;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO for updating {@link org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun}
 */
public record UpdateWorkflowRunRequest(WorkflowStatus status, String error, TriggerType triggerType,
                                      OffsetDateTime endedAt) implements Serializable {
}
