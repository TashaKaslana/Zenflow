package org.phong.zenflow.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.dto.CreateWorkflowRequest;
import org.phong.zenflow.workflow.dto.UpdateWorkflowRequest;
import org.phong.zenflow.workflow.dto.WorkflowDefinitionChangeRequest;
import org.phong.zenflow.workflow.dto.UpdateWorkflowDefinitionResponse;
import org.phong.zenflow.workflow.dto.WorkflowDto;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.dto.ExecuteWorkflowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.service.WorkflowValidationService;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowValidationService validationService;

    @PostMapping
    public ResponseEntity<RestApiResponse<WorkflowDto>> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowDto createdWorkflow = workflowService.createWorkflow(request);
        return RestApiResponse.created(createdWorkflow, "Workflow created successfully");
    }

    @PostMapping("/bulk")
    public ResponseEntity<RestApiResponse<List<WorkflowDto>>> createWorkflows(@Valid @RequestBody List<CreateWorkflowRequest> requests) {
        List<WorkflowDto> createdWorkflows = workflowService.createWorkflows(requests);
        return RestApiResponse.created(createdWorkflows, "Workflows created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<WorkflowDto>> getWorkflowById(@PathVariable UUID id) {
        WorkflowDto workflow = workflowService.findById(id);
        return RestApiResponse.success(workflow, "Workflow retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<WorkflowDto>>> getAllWorkflows() {
        List<WorkflowDto> workflows = workflowService.findAll();
        return RestApiResponse.success(workflows, "Workflows retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<WorkflowDto>>> getAllWorkflowsPaginated(Pageable pageable) {
        Page<WorkflowDto> workflows = workflowService.findAll(pageable);
        return RestApiResponse.success(workflows, "Workflows retrieved successfully");
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<RestApiResponse<List<WorkflowDto>>> getWorkflowsByProjectId(@PathVariable UUID projectId) {
        List<WorkflowDto> workflows = workflowService.findByProjectId(projectId);
        return RestApiResponse.success(workflows, "Project workflows retrieved successfully");
    }

    @GetMapping("/project/{projectId}/active")
    public ResponseEntity<RestApiResponse<List<WorkflowDto>>> getActiveWorkflowsByProjectId(@PathVariable UUID projectId) {
        List<WorkflowDto> workflows = workflowService.findActiveByProjectId(projectId);
        return RestApiResponse.success(workflows, "Active project workflows retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<WorkflowDto>> updateWorkflow(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkflowRequest request) {
        WorkflowDto updatedWorkflow = workflowService.updateWorkflow(id, request);
        return RestApiResponse.success(updatedWorkflow, "Workflow updated successfully");
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<RestApiResponse<WorkflowDto>> activateWorkflow(@PathVariable UUID id) {
        WorkflowDto workflow = workflowService.activateWorkflow(id);
        return RestApiResponse.success(workflow, "Workflow activated successfully");
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<RestApiResponse<WorkflowDto>> deactivateWorkflow(@PathVariable UUID id) {
        WorkflowDto workflow = workflowService.deactivateWorkflow(id);
        return RestApiResponse.success(workflow, "Workflow deactivated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteWorkflow(@PathVariable UUID id) {
        workflowService.deleteWorkflow(id);
        return RestApiResponse.noContent();
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<RestApiResponse<Void>> hardDeleteWorkflow(@PathVariable UUID id) {
        workflowService.hardDeleteWorkflow(id);
        return RestApiResponse.noContent();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkWorkflowExists(@PathVariable UUID id) {
        boolean exists = workflowService.existsById(id);
        return RestApiResponse.success(exists, "Workflow existence checked");
    }

    @GetMapping("/project/{projectId}/count")
    public ResponseEntity<RestApiResponse<Long>> countWorkflowsByProjectId(@PathVariable UUID projectId) {
        long count = workflowService.countByProjectId(projectId);
        return RestApiResponse.success(count, "Workflow count retrieved successfully");
    }

    @GetMapping("/project/{projectId}/count/active")
    public ResponseEntity<RestApiResponse<Long>> countActiveWorkflowsByProjectId(@PathVariable UUID projectId) {
        long count = workflowService.countActiveByProjectId(projectId);
        return RestApiResponse.success(count, "Active workflow count retrieved successfully");
    }

    @PostMapping("/{id}/nodes")
    @Transactional
    public ResponseEntity<RestApiResponse<UpdateWorkflowDefinitionResponse>> updateWorkflowDefinition(
            @PathVariable UUID id,
            @RequestBody WorkflowDefinitionChangeRequest request,
            @RequestParam(name = "publish", defaultValue = "false") boolean publish) {
        WorkflowDefinition updatedDefinition = workflowService.updateWorkflowDefinition(id, request, publish);
        ValidationResult definitionVr = validationService.validateDefinition(id, updatedDefinition, false);
        ValidationResult publishVr = validationService.validateDefinition(id, updatedDefinition, true);
        Boolean isActive = workflowService.getWorkflow(id).getIsActive();
        UpdateWorkflowDefinitionResponse body = new UpdateWorkflowDefinitionResponse(updatedDefinition, isActive, definitionVr, publishVr);
        return RestApiResponse.success(body, "Workflow definition updated successfully");
    }

    @PostMapping("/{id}/nodes/{nodeKey}/execute")
    public ResponseEntity<RestApiResponse<ExecuteWorkflowResponse>> executeWorkflow(@PathVariable UUID id, @PathVariable String nodeKey) {
        UUID workflowRunId = workflowService.executeWorkflow(id, nodeKey);
        return RestApiResponse.success(
                ExecuteWorkflowResponse.of(workflowRunId),
                "Workflow executed successfully"
        );
    }

    @DeleteMapping("/{id}/nodes/clear")
    public ResponseEntity<RestApiResponse<WorkflowDefinition>> clearWorkflowDefinition(
            @PathVariable UUID id) {
        WorkflowDefinition clearedDefinition = workflowService.clearWorkflowDefinition(id);
        return RestApiResponse.success(clearedDefinition, "Workflow definition cleared successfully");
    }
}
