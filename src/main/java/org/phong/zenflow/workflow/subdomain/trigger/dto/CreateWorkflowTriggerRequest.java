package org.phong.zenflow.workflow.subdomain.trigger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.util.Map;
import java.util.UUID;

@Data
public class CreateWorkflowTriggerRequest {
    @NotNull
    private UUID workflowId;

    @NotNull
    private TriggerType type;

    private Map<String, Object> config;

    private Boolean enabled = true;
    private UUID triggerExecutorId;
}
