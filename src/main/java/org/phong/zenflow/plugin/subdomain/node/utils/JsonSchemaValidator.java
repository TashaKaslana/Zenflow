package org.phong.zenflow.plugin.subdomain.node.utils;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class JsonSchemaValidator {

    public static void validate(JSONObject instance, JSONObject schemaJson) {
        Schema schema = SchemaLoader.load(schemaJson);
        schema.validate(instance);
    }
}
