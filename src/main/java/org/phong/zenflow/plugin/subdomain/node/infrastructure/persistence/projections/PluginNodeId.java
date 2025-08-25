package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections;


import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public interface PluginNodeId {
    UUID getId();

    String getCompositeKey();
}
