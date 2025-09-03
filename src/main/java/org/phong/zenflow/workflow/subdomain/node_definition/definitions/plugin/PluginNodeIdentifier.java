package org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public final class PluginNodeIdentifier {
    private UUID nodeId;
    private final String pluginKey;
    private final String nodeKey;
    private final String version;
    private final String executorType;

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

    public PluginNodeIdentifier(String pluginKey,
                                String nodeKey,
                                String version,
                                String executorType) {
        this(null, pluginKey, nodeKey, version, executorType);
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
