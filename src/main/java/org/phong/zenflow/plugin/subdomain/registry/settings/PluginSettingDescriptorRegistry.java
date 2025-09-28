package org.phong.zenflow.plugin.subdomain.registry.settings;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry exposing plugin setting descriptors at runtime.
 */
@Component
public class PluginSettingDescriptorRegistry {

    private final Map<UUID, List<RegisteredPluginSettingDescriptor>> descriptorsByPluginId = new ConcurrentHashMap<>();
    private final Map<String, List<RegisteredPluginSettingDescriptor>> descriptorsByPluginKey = new ConcurrentHashMap<>();

    public void register(UUID pluginId, String pluginKey, List<RegisteredPluginSettingDescriptor> descriptors) {
        List<RegisteredPluginSettingDescriptor> immutable = Collections.unmodifiableList(descriptors);
        if (pluginId != null) {
            descriptorsByPluginId.put(pluginId, immutable);
        }
        if (pluginKey != null) {
            descriptorsByPluginKey.put(pluginKey, immutable);
        }
    }

    public List<RegisteredPluginSettingDescriptor> getByPluginId(UUID pluginId) {
        return descriptorsByPluginId.getOrDefault(pluginId, List.of());
    }

    public List<RegisteredPluginSettingDescriptor> getByPluginKey(String pluginKey) {
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
