package org.phong.zenflow.workflow.subdomain.worker.node_gateway.event;

import lombok.Data;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Data
public class WorkflowStart {
    @NotNull
    private UUID workflowId;

    @Nullable
    private Map<String, Object> payload;

    @Nullable
    private String callbackUrl;

    @NotNull
    private TriggerType triggerType = TriggerType.MANUAL;
}
