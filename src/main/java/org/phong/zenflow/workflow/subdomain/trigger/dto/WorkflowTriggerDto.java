package org.phong.zenflow.workflow.subdomain.trigger.dto;

import lombok.Data;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class WorkflowTriggerDto {
    private UUID id;
    private UUID workflowId;
    private TriggerType type;
    private Map<String, Object> config;
    private Boolean enabled;
    private OffsetDateTime lastTriggeredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
