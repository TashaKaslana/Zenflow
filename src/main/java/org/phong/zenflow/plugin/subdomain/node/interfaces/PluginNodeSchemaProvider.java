package org.phong.zenflow.plugin.subdomain.node.interfaces;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PluginNodeSchemaProvider {
    Map<String, Object> getSchemaJson(UUID nodeId);
    Map<UUID, Map<String, Object>> getAllSchemasByNodeIds(List<UUID> nodeIds);
}
