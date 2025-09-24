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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            Map<String, RegisteredPluginProfileDescriptor> descriptorsById = profiles.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            descriptor -> descriptor.descriptor().id(),
                            Function.identity(),
                            (existing, duplicate) -> existing,
                            LinkedHashMap::new
                    ));

            List<Map<String, Object>> profileMetadata = new ArrayList<>();
            Map<String, Object> firstProfileSchema = null;
            for (RegisteredPluginProfileDescriptor descriptor : descriptorsById.values()) {
                Map<String, Object> metadata = new LinkedHashMap<>(descriptor.asMetadataMap());
                metadata.put("pluginKey", annotation.key());
                metadata.put("namespacedId", annotation.key() + "." + descriptor.descriptor().id());
                profileMetadata.add(metadata);
                if (firstProfileSchema == null && !descriptor.schema().isEmpty()) {
                    firstProfileSchema = new LinkedHashMap<>(descriptor.schema());
                }
            }
            combined.put("profiles", profileMetadata);

            if (!combined.containsKey("profile") && firstProfileSchema != null) {
                combined.put("profile", firstProfileSchema);
            }

            enrichProfileSection(combined, annotation.key(), descriptorsById);
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

    private void enrichProfileSection(
            Map<String, Object> combined,
            String pluginKey,
            Map<String, RegisteredPluginProfileDescriptor> descriptorsById
    ) {
        Object profileObj = combined.get("profile");
        if (!(profileObj instanceof Map<?, ?> rawProfile)) {
            return;
        }

        Map<String, Object> profileSection = new LinkedHashMap<>();
        rawProfile.forEach((k, v) -> profileSection.put(String.valueOf(k), v));

        List<String> profileKeys = extractProfileKeys(profileSection.get("profileKeys"));
        if (profileKeys.isEmpty()) {
            combined.put("profile", profileSection);
            return;
        }

        List<Map<String, Object>> resolvedProfiles = new ArrayList<>();
        List<String> namespacedKeys = new ArrayList<>();
        List<String> missingDescriptors = new ArrayList<>();

        for (String key : profileKeys) {
            RegisteredPluginProfileDescriptor descriptor = descriptorsById.get(key);
            if (descriptor == null) {
                missingDescriptors.add(key);
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>(descriptor.asMetadataMap());
            String namespacedId = pluginKey + "." + descriptor.descriptor().id();
            metadata.put("pluginKey", pluginKey);
            metadata.put("namespacedId", namespacedId);
            resolvedProfiles.add(metadata);
            namespacedKeys.add(namespacedId);
        }

        if (!missingDescriptors.isEmpty()) {
            profileSection.put("missingDescriptors", Collections.unmodifiableList(missingDescriptors));
        }

        if (!resolvedProfiles.isEmpty()) {
            profileSection.put("resolvedProfiles", resolvedProfiles);
            if (!profileSection.containsKey("schema") && resolvedProfiles.size() == 1) {
                Object schema = resolvedProfiles.getFirst().get("schema");
                if (schema instanceof Map<?, ?> schemaMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> schemaCopy = new LinkedHashMap<>((Map<String, Object>) schemaMap);
                    profileSection.put("schema", schemaCopy);
                }
            }
            profileSection.put("namespacedKeys", Collections.unmodifiableList(namespacedKeys));
        }

        profileSection.put("profileKeys", Collections.unmodifiableList(profileKeys));
        combined.put("profile", profileSection);
    }

    private List<String> extractProfileKeys(Object rawKeys) {
        if (!(rawKeys instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String str && !str.isBlank()) {
                keys.add(str);
            } else if (entry instanceof Map<?, ?> map) {
                Object ref = map.get("refId");
                if (ref instanceof String str && !str.isBlank()) {
                    keys.add(str);
                }
            }
        }
        return keys;
    }
}
