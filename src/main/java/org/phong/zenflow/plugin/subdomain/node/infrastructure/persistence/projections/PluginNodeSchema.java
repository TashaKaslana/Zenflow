package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections;

import java.util.Map;

/**
 * Projection for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public interface PluginNodeSchema {
    String getPluginKey();
    String getNodeKey();
    String getVersion();
    Map<String, Object> getConfigSchema();

    default String getCacheKey() {
        return getPluginKey() + ":" + getNodeKey() + ":" + getVersion();
    }
}