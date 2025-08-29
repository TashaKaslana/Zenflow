package org.phong.zenflow.workflow.subdomain.trigger.dto;

import lombok.Data;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.util.Map;
import java.util.UUID;

@Data
public class UpdateWorkflowTriggerRequest {
    private TriggerType type;
    private Map<String, Object> config;
    private Boolean enabled;
    private UUID triggerExecutorId;
}
