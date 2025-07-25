package org.phong.zenflow.workflow.subdomain.schema_validator.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationError {
    private String type;
    private String path;
    private String message;
    private Object value;
    private String template;
    private String expectedType;
    private String schemaPath;
}
