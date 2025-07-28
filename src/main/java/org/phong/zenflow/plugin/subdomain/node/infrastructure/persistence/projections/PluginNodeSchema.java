package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections;

import java.util.Map;

/**
 * Projection for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public interface PluginNodeSchema {
    String getKey();

    Map<String, Object> getConfigSchema();
}