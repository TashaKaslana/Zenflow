package org.phong.zenflow.workflow.subdomain.trigger.resource;

import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;

/**
 * Configuration interface for trigger resources, following the same pattern as ResolvedDbConfig.
 * This is generic and flexible to support any trigger type.
 */
public interface TriggerResourceConfig extends ResourceConfig {
    /**
     * Create from WorkflowTrigger, similar to ResolvedDbConfig.fromInput()
     */
    static DefaultResourceConfig fromTrigger(TriggerContext triggerCtx, String resourceKeyField) {
        return new DefaultResourceConfig(triggerCtx, resourceKeyField);
    }
}
