package org.phong.zenflow.secret.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecretProfileSchemaValidator {
    private final SchemaRegistry schemaRegistry;

    public boolean validate(UUID pluginId, UUID pluginNodeId, Map<String, String> secrets) {
        try {
            JSONObject schemaObject = null;
            if (pluginId != null) {
                schemaObject = schemaRegistry.getSchemaByTemplateString("plugin:" + pluginId);
            }
            if (schemaObject == null && pluginNodeId != null) {
                schemaObject = schemaRegistry.getSchemaByTemplateString(pluginNodeId.toString());
            }

            if (schemaObject == null) {
                return false;
            }

            JSONObject secretsJson = getProfileItemsDefinition(schemaObject);
            if (secretsJson == null) {
                log.warn("Could not extract profile items definition from schema");
                return false;
            }

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

    public JSONObject getProfileItemsDefinition(JSONObject schema) {
        try {
            return schema.getJSONObject("properties")
                    .getJSONObject("profile");
        } catch (Exception e) {
            return null;
        }
    }
}
