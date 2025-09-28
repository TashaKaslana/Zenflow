package org.phong.zenflow.workflow.subdomain.schema_validator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
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
        // Ensure we always have a mutable list to avoid UnsupportedOperationException
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.valid = this.errors.isEmpty();
    }

    public void addAllErrors(List<ValidationError> errors) {
        if (errors != null && !errors.isEmpty()) {
            // Ensure we have a mutable list before adding
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.addAll(errors);
            this.valid = this.errors.isEmpty();
        }
    }
}
