package org.phong.zenflow.plugin.subdomain.resource.trigger;

import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;

import java.util.Map;


/**
 * An equivalent to void(null) resource config, for triggers that do not require any resource configuration.
 * This is useful for triggers nodes
 */
public final class VoidResourceConfig implements ResourceConfig {
    @Override
    public String getResourceIdentifier() {
        return "void";
    }

    @Override
    public Map<String, Object> getContextMap() {
        return Map.of();
    }
}
