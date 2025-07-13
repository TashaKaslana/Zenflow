package org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin;


import java.util.UUID;

public record PluginNodeDefinition(UUID pluginId, String pluginName, String pluginVersion,
                                   String nodeId, String nodeName, String nodeVersion) {
}
