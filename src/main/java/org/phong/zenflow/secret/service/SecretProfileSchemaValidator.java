package org.phong.zenflow.secret.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.schema.services.SchemaRegistry;
import org.phong.zenflow.secret.dto.CreateProfileSecretsRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecretProfileSchemaValidator {
    private final SchemaRegistry schemaRegistry;

    public boolean validate(UUID pluginNodeId, List<CreateProfileSecretsRequest.SecretEntry> secrets) {
        try {
            JSONObject schemaObject = schemaRegistry.getSchemaByTemplateString(
                    pluginNodeId.toString()
            );

            if (schemaObject == null) {
                return false;
            }

            JSONObject secretsJson = new JSONObject(getProfileItemsDefinition(schemaObject));

            Schema schema = SchemaLoader.builder()
                    .schemaJson(secretsJson)
                    .draftV7Support()
                    .resolutionScope("classpath:/schemas/")
                    .build()
                    .load()
                    .build();

            schema.validate(secrets);

            return true;
        } catch (Exception e) {
            log.error("Error validating secrets against schema", e);
            return false;
        }
    }

    public JSONObject getProfileItemsDefinition(JSONObject schema) {
        try {
            return schema.getJSONObject("properties")
                    .getJSONObject("profiles")
                    .getJSONObject("properties")
                    .getJSONObject("items");
        } catch (Exception e) {
            return null;
        }
    }
}
