package org.phong.zenflow.workflow.subdomain.trigger.resource;

import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;

import java.util.Map;

/**
 * Default implementation of TriggerResourceConfig that wraps WorkflowTrigger.config
 */
public class DefaultTriggerResourceConfig implements TriggerResourceConfig {

    private final Map<String, Object> configMap;
    private final String resourceIdentifier;

    public DefaultTriggerResourceConfig(WorkflowTrigger trigger, String resourceKeyField) {
        this.configMap = trigger.getConfig();
        this.resourceIdentifier = extractResourceIdentifier(resourceKeyField);
    }

    public DefaultTriggerResourceConfig(Map<String, Object> config, String resourceKeyField) {
        this.configMap = config;
        this.resourceIdentifier = extractResourceIdentifier(resourceKeyField);
    }

    @Override
    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public Map<String, Object> getConfigMap() {
        return configMap;
    }

    private String extractResourceIdentifier(String keyField) {
        Object value = configMap.get(keyField);
        if (value == null) {
            throw new IllegalArgumentException("Resource key field '" + keyField + "' not found in config");
        }
        return value.toString();
    }
}
