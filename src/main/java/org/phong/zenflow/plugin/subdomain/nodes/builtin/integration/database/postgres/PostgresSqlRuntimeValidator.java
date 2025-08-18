package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.postgres;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PostgresSqlRuntimeValidator {

    public List<ValidationError> validate(WorkflowConfig config, RuntimeContext ctx) {
        List<ValidationError> errors = new ArrayList<>();
        Map<String, Object> input = config != null ? config.input() : null;
        if (input == null) {
            errors.add(ValidationError.builder()
                    .errorType("runtime")
                    .errorCode(ValidationErrorCode.MISSING_REQUIRED_FIELD)
                    .path("config.input")
                    .message("Input configuration is required")
                    .build());
            return errors;
        }

        Object query = input.get("query");
        if (!(query instanceof String) || ((String) query).isBlank()) {
            errors.add(ValidationError.builder()
                    .errorType("runtime")
                    .errorCode(ValidationErrorCode.MISSING_REQUIRED_FIELD)
                    .path("config.input.query")
                    .message("Query is required")
                    .build());
        } else {
            try {
                CCJSqlParserUtil.parse((String) query);
            } catch (JSQLParserException e) {
                errors.add(ValidationError.builder()
                        .errorType("runtime")
                        .errorCode(ValidationErrorCode.INVALID_VALUE)
                        .path("config.input.query")
                        .message("Invalid SQL syntax: " + e.getMessage())
                        .build());
            }
        }
        return errors;
    }
}

