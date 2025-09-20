package org.phong.zenflow.plugin.subdomain.registry;

import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.subdomain.registry.profile.RegisteredPluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.registry.settings.RegisteredPluginSettingDescriptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the persisted plugin schema document by combining base schemas with descriptor metadata.
 */

@Component
public class PluginSchemaComposer {

    public Map<String, Object> compose(
            Class<?> pluginClass,
            org.phong.zenflow.plugin.subdomain.registry.Plugin annotation,
            List<RegisteredPluginProfileDescriptor> profiles,
            List<RegisteredPluginSettingDescriptor> settings
    ) {
        Map<String, Object> baseSchema = loadBaseSchema(pluginClass, annotation.schemaPath());
        LinkedHashMap<String, Object> combined = new LinkedHashMap<>(baseSchema);

        if (profiles != null && !profiles.isEmpty()) {
            List<Map<String, Object>> profileMetadata = new ArrayList<>();
            Map<String, Object> firstProfileSchema = null;
            for (RegisteredPluginProfileDescriptor descriptor : profiles) {
                profileMetadata.add(descriptor.asMetadataMap());
                if (firstProfileSchema == null && !descriptor.schema().isEmpty()) {
                    firstProfileSchema = descriptor.schema();
                }
            }
            combined.put("profiles", profileMetadata);
            if (!combined.containsKey("profile") && firstProfileSchema != null) {
                combined.put("profile", firstProfileSchema);
            }
        }

        if (settings != null && !settings.isEmpty()) {
            List<Map<String, Object>> settingsMetadata = new ArrayList<>();
            for (RegisteredPluginSettingDescriptor descriptor : settings) {
                settingsMetadata.add(descriptor.asMetadataMap());
            }
            combined.put("settings", settingsMetadata);
        }

        return combined.isEmpty() ? Collections.emptyMap() : combined;
    }

    private Map<String, Object> loadBaseSchema(Class<?> pluginClass, String schemaPath) {
        if (schemaPath == null || schemaPath.trim().isEmpty()) {
            return Map.of();
        }
        return LoadSchemaHelper.loadSchema(pluginClass, schemaPath.trim(), "plugin.schema.json");
    }
}
