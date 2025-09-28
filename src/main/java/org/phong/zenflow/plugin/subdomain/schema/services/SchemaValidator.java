package org.phong.zenflow.plugin.subdomain.schema.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@AllArgsConstructor
public class SchemaValidator {
    private final SchemaRegistry schemaRegistry;

    public boolean validate(JSONObject schemaDefinition, JSONObject instanceSchema) {
        try {
            Schema schema = SchemaLoader.builder()
                    .schemaJson(schemaDefinition)
                    .draftV7Support()
                    .resolutionScope("classpath:/builtin_schemas/")
                    .build()
                    .load()
                    .build();

            schema.validate(instanceSchema);
            return true;
        } catch (ValidationException e) {
            log.error("Schema validation failed when compared two objects: {}", e.getMessage());
            int order = 1;
            for (ValidationException ve : e.getCausingExceptions()) {
                log.error("{} - {}", order++, ve.getMessage());
            }
            return false;
        }
    }

    public boolean validate(String schemaTemplateString, JSONObject instanceSchema) {
        try {
            JSONObject schemaDefinition = schemaRegistry.getSchemaByTemplateString(schemaTemplateString);
            if (schemaDefinition == null) {
                log.error("Schema not found: {}", schemaTemplateString);
                return false;
            }
            return validate(schemaDefinition, instanceSchema);
        } catch (Exception e) {
            log.error("Schema validation failed: {}", e.getMessage());
            return false;
        }
    }
}
