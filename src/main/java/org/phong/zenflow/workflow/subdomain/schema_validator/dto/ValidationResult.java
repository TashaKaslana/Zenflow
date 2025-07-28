package org.phong.zenflow.workflow.subdomain.schema_validator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {
    private String phase;
    private List<ValidationError> errors;
    private boolean valid;

    public ValidationResult(String phase, List<ValidationError> errors) {
        this.phase = phase;
        this.errors = errors;
        this.valid = errors.isEmpty();
    }
}
