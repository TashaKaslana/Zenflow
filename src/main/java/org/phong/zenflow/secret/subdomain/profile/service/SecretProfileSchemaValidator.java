package org.phong.zenflow.secret.subdomain.profile.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.registry.profile.PluginProfileDescriptorRegistry;
import org.phong.zenflow.plugin.subdomain.registry.profile.RegisteredPluginProfileDescriptor;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecretProfileSchemaValidator {
    private final SchemaRegistry schemaRegistry;
    private final PluginProfileDescriptorRegistry profileDescriptorRegistry;

    public boolean validate(UUID pluginId, UUID pluginNodeId, Map<String, String> secrets) {
        try {
            Map<String, Object> combinedSchema = getCombinedSchema(pluginId, pluginNodeId);
            if (combinedSchema == null) return false;

            JSONObject secretsJson = new JSONObject(combinedSchema);
            JSONObject subject = new JSONObject(secrets);

            Schema schema = SchemaLoader.builder()
                    .schemaJson(secretsJson)
                    .draftV7Support()
                    .resolutionScope("classpath:/schemas/")
                    .build()
                    .load()
                    .build();

            schema.validate(subject);

            return true;
        } catch (Exception e) {
            log.error("Error validating secrets against schema", e);
            return false;
        }
    }

    @Nullable
    private Map<String, Object> getCombinedSchema(UUID pluginId, UUID pluginNodeId) {
        JSONObject nodeSchemaObj;

        nodeSchemaObj = schemaRegistry.getSchemaByTemplateString(pluginNodeId.toString());
        List<String> profileKeys = getRequiredProfileKeys(nodeSchemaObj);

        if (profileKeys == null || profileKeys.isEmpty()) {
            log.warn("No profile keys defined in node schema");
            return null;
        }

        List<RegisteredPluginProfileDescriptor> profileDescriptors = profileDescriptorRegistry.getByPluginId(pluginId);
        List<Map<String, Object>> descriptorSchemas = profileDescriptors.stream()
                .filter(descriptorCompose
                        -> profileKeys.contains(descriptorCompose.descriptor().id())
                )
                .map(RegisteredPluginProfileDescriptor::schema)
                .toList();
        if (descriptorSchemas.isEmpty()) {
            log.warn("No matching profile descriptors found for plugin {}", pluginId);
            return null;
        }

        Map<String, Object> combinedSchema = new HashMap<>();

        descriptorSchemas.forEach(schema -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) schema.get("properties");
            if (props != null) {
                combinedSchema.putAll(props);
            }
        });
        return combinedSchema;
    }

    public List<String> getRequiredProfileKeys(JSONObject schema) {
        try {
            return schema.getJSONObject("properties")
                    .getJSONObject("profile")
                    .getJSONArray("profileKeys")
                    .toList()
                    .stream()
                    .map(Object::toString)
                    .toList();
        } catch (Exception e) {
            return null;
        }
    }
}
