package org.phong.zenflow.plugin.subdomain.node.interfaces;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;

import java.util.List;
import java.util.Map;

public interface PluginNodeSchemaProvider {
    Map<String, Object> getSchemaJson(String key);
    Map<String, Map<String, Object>> getAllSchemasByIdentifiers(List<PluginNodeIdentifier> identifiers);
}
