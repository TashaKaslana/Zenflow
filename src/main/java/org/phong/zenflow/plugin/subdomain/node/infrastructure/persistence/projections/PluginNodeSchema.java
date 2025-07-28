package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections;

import java.util.Map;
import java.util.UUID;

/**
 * Projection for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public interface PluginNodeSchema {
    UUID getId();

    Map<String, Object> getConfigSchema();
}