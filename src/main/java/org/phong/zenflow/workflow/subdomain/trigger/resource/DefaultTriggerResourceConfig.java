package org.phong.zenflow.workflow.subdomain.trigger.resource;

import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of TriggerResourceConfig that wraps WorkflowTrigger.config
 */
public class DefaultTriggerResourceConfig implements TriggerResourceConfig {

    private final Map<String, Object> contextMap;
    private final String resourceIdentifier;

    //TODO: currently overwrites contextMap with profiles if present. Consider merging strategies.
    public DefaultTriggerResourceConfig(TriggerContext triggerCtx, String resourceKeyField) {
        Map<String, Object> mergeContext = new HashMap<>(triggerCtx.trigger().getConfig());
        if (triggerCtx.profiles() != null) {
            mergeContext.putAll(triggerCtx.profiles());
        }
        this.contextMap = mergeContext;
        this.resourceIdentifier = extractResourceIdentifier(resourceKeyField);
    }

    public DefaultTriggerResourceConfig(Map<String, Object> config, String resourceKeyField) {
        this.contextMap = config;
        this.resourceIdentifier = extractResourceIdentifier(resourceKeyField);
    }

    @Override
    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    private String extractResourceIdentifier(String keyField) {
        Object value = contextMap.get(keyField);
        if (value == null) {
            throw new IllegalArgumentException("Resource key field '" + keyField + "' not found in config");
        }
        return value.toString();
    }
}
