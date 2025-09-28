package org.phong.zenflow.plugin.subdomain.registry.profile;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry that exposes plugin profile descriptors during runtime.
 */
@Component
public class PluginProfileDescriptorRegistry {

    private final Map<UUID, List<RegisteredPluginProfileDescriptor>> descriptorsByPluginId = new ConcurrentHashMap<>();
    private final Map<String, List<RegisteredPluginProfileDescriptor>> descriptorsByPluginKey = new ConcurrentHashMap<>();

    public void register(UUID pluginId, String pluginKey, List<RegisteredPluginProfileDescriptor> descriptors) {
        List<RegisteredPluginProfileDescriptor> immutable = Collections.unmodifiableList(descriptors);
        if (pluginId != null) {
            descriptorsByPluginId.put(pluginId, immutable);
        }
        if (pluginKey != null) {
            descriptorsByPluginKey.put(pluginKey, immutable);
        }
    }

    public List<RegisteredPluginProfileDescriptor> getByPluginId(UUID pluginId) {
        return descriptorsByPluginId.getOrDefault(pluginId, List.of());
    }

    public List<RegisteredPluginProfileDescriptor> getByPluginKey(String pluginKey) {
        if (pluginKey == null) {
            return List.of();
        }
        return descriptorsByPluginKey.getOrDefault(pluginKey, List.of());
    }

    public void clear(UUID pluginId, String pluginKey) {
        if (pluginId != null) {
            descriptorsByPluginId.remove(pluginId);
        }
        if (pluginKey != null) {
            descriptorsByPluginKey.remove(pluginKey);
        }
    }
}
