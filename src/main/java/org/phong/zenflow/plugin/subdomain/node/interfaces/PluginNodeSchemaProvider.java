package org.phong.zenflow.plugin.subdomain.node.interfaces;

import java.util.UUID;

public interface PluginNodeSchemaProvider {
    String getSchemaJson(UUID pluginId, UUID nodeId);
}
