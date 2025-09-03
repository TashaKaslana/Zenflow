package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.schedule;

import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceKey;

import java.util.Map;

/**
 * Resource configuration for schedule triggers.
 * Unlike database connections which need different resource keys based on connection parameters,
 * schedule triggers can all share the same Quartz scheduler.
 */
public class ScheduleTriggerResourceConfig implements TriggerResourceConfig {

    private final Map<String, Object> configMap;
    private final String resourceIdentifier;

    public ScheduleTriggerResourceConfig(WorkflowTrigger trigger) {
        this.configMap = trigger.getConfig();
        // All schedule triggers use the same resource identifier since they share the scheduler
        this.resourceIdentifier = "default-scheduler";
    }

    @Override
    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    @Override
    public TriggerResourceKey toResourceKey() {
        // For schedule triggers, the resource key is simple since they all share the same scheduler
        return new TriggerResourceKey(resourceIdentifier);
    }
}
