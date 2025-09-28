package org.phong.zenflow.plugin.subdomain.schema.utils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.exception.PluginNodeException;

import java.util.Map;

public class JsonSchemaValidator {

    public static void validate(JSONObject schema, Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            throw new PluginNodeException("Configuration cannot be null or empty.");
        }
        validate(schema, new JSONObject(config));
    }

    public static void validate(JSONObject schema, JSONObject config) {
        if (config == null || config.isEmpty()) {
            throw new PluginNodeException("Configuration cannot be null or empty.");
        }
        try {
            Schema jsonSchema = SchemaLoader.load(schema);
            jsonSchema.validate(config);
        } catch (ValidationException e) {
            throw new PluginNodeException("JSON schema validation failed: " + e.getMessage());
        }
    }
}
