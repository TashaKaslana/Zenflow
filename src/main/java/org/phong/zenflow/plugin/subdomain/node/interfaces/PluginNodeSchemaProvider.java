package org.phong.zenflow.plugin.subdomain.node.interfaces;

import java.util.Map;
import java.util.UUID;

public interface PluginNodeSchemaProvider {
    Map<String, Object> getSchemaJson(UUID pluginId, UUID nodeId);
}
