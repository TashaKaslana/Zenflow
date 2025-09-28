package org.phong.zenflow.workflow.subdomain.schema_validator.dto;

import lombok.Builder;
import lombok.Data;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;

@Data
@Builder
public class ValidationError {
    private String nodeKey;
    private ValidationErrorCode errorCode;
    private String errorType;
    private String path;
    private String message;
    private Object value;
    private String template;
    private String expectedType;
    private String schemaPath;
}
