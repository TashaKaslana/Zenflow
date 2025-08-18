package org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin;


import lombok.NonNull;

public record PluginNodeIdentifier(String pluginKey, String nodeKey, String version, String executorType) {
    public static PluginNodeIdentifier fromString(String s) {
        return fromString(s, null);
    }

    public static PluginNodeIdentifier fromString(String s, String executorType) {
        String[] parts = s.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid PluginNodeIdentifier string format: " + s + ". Expected format: <pluginKey>:<nodeKey>:<version>");
        }
        return new PluginNodeIdentifier(parts[0], parts[1], parts[2], executorType);
    }

    public String toCacheKey() {
        return pluginKey + ":" + nodeKey + ":" + version;
    }

    @NonNull
    @Override
    public String toString() {
        return pluginKey + ":" + nodeKey + ":" + version;
    }
}
