package org.phong.zenflow.workflow.subdomain.schema_validator.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowValidationController {

    private final WorkflowService workflowService;
    private final WorkflowValidationService validationService;

    /**
     * Preflight validation endpoint. Supports phases:
     * - definition: structural + non-strict reference checks (warnings)
     * - publish: structural + strict reference checks (errors)
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<RestApiResponse<ValidationResult>> validate(
            @PathVariable UUID id,
            @RequestParam(name = "phase", defaultValue = "definition") String phase
    ) {
        Workflow wf = workflowService.getWorkflow(id);
        String norm = phase.toLowerCase(Locale.ROOT);
        ValidationResult result;
        switch (norm) {
            case "publish" -> result = validationService.validateDefinition(id, wf.getDefinition(), true);
            case "definition" -> result = validationService.validateDefinition(id, wf.getDefinition(), false);
            default -> result = validationService.validateDefinition(wf.getDefinition());
        }
        return RestApiResponse.success(result, "Validation completed");
    }
}

