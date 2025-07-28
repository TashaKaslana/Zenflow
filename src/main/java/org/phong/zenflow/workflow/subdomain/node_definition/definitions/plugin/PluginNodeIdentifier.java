package org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin;



public record PluginNodeIdentifier(String pluginKey, String nodeKey, String executorType) {
    public static PluginNodeIdentifier fromString(String s) {
        return fromString(s, null);
    }

    public static PluginNodeIdentifier fromString(String s, String executorType) {
        String[] parts = s.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid PluginNodeIdentifier string format: " + s);
        }
        return new PluginNodeIdentifier(parts[0], parts[1], executorType);
    }

    public String toCacheKey() {
        return pluginKey + ":" + nodeKey;
    }
}
