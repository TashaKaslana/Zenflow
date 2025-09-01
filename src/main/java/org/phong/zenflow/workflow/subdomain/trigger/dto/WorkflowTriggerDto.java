package org.phong.zenflow.workflow.subdomain.trigger.dto;

import lombok.Data;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for creating {@link org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger}
 */
@Data
public class WorkflowTriggerDto {
    private UUID id;
    private UUID workflowId;
    private TriggerType type;
    private Map<String, Object> config;
    private Boolean enabled;
    private Instant lastTriggeredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private UUID triggerExecutorId;
}
