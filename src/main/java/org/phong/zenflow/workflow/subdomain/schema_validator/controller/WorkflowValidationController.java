package org.phong.zenflow.workflow.subdomain.schema_validator.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowValidationController {

    private final WorkflowService workflowService;
    private final WorkflowValidationService validationService;

    /**
     * Preflight validation endpoint.
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<RestApiResponse<ValidationResult>> validate(
            @PathVariable UUID id) {
        Workflow wf = workflowService.getWorkflow(id);
        ValidationResult result = validationService.validateDefinition(id, wf.getDefinition());
        return RestApiResponse.success(result, "Validation completed");
    }
}

