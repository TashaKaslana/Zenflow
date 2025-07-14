package org.phong.zenflow.workflow.subdomain.trigger.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.CreateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.UpdateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerDto;
import org.phong.zenflow.workflow.subdomain.trigger.services.WorkflowTriggerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/workflows/triggers")
@RequiredArgsConstructor
@Tag(name = "Workflow Triggers", description = "Endpoints for managing workflow triggers")
public class WorkflowTriggerController {

    private final WorkflowTriggerService triggerService;

    @Operation(summary = "Create a new workflow trigger")
    @PostMapping
    public ResponseEntity<RestApiResponse<WorkflowTriggerDto>> createTrigger(
            @Valid @RequestBody CreateWorkflowTriggerRequest request) {
        log.info("Request to create workflow trigger for workflow ID: {}", request.getWorkflowId());

        WorkflowTriggerDto trigger = triggerService.createTrigger(request);

        return RestApiResponse.created(trigger, "Workflow trigger created successfully");
    }

    @Operation(summary = "Get a workflow trigger by ID")
    @GetMapping("/{triggerId}")
    public ResponseEntity<RestApiResponse<WorkflowTriggerDto>> getTriggerById(
            @Parameter(description = "Trigger ID") @PathVariable UUID triggerId) {
        log.info("Request to get workflow trigger with ID: {}", triggerId);

        WorkflowTriggerDto trigger = triggerService.getTriggerById(triggerId);

        return RestApiResponse.success(trigger, "Workflow trigger retrieved successfully");
    }

    @Operation(summary = "Get all workflow triggers")
    @GetMapping
    public ResponseEntity<RestApiResponse<List<WorkflowTriggerDto>>> getAllTriggers(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Request to get all workflow triggers");

        Page<WorkflowTriggerDto> triggers = triggerService.getAllTriggers(pageable);

        return RestApiResponse.success(triggers, "Workflow triggers retrieved successfully");
    }

    @Operation(summary = "Get workflow triggers by workflow ID")
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<RestApiResponse<List<WorkflowTriggerDto>>> getTriggersByWorkflowId(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {
        log.info("Request to get triggers for workflow ID: {}", workflowId);

        List<WorkflowTriggerDto> triggers = triggerService.getTriggersByWorkflowId(workflowId);

        return RestApiResponse.success(triggers, "Workflow triggers retrieved successfully");
    }

    @Operation(summary = "Update a workflow trigger")
    @PutMapping("/{triggerId}")
    public ResponseEntity<RestApiResponse<WorkflowTriggerDto>> updateTrigger(
            @Parameter(description = "Trigger ID") @PathVariable UUID triggerId,
            @Valid @RequestBody UpdateWorkflowTriggerRequest request) {
        log.info("Request to update workflow trigger with ID: {}", triggerId);

        WorkflowTriggerDto trigger = triggerService.updateTrigger(triggerId, request);

        return RestApiResponse.success(trigger, "Workflow trigger updated successfully");
    }

    @Operation(summary = "Delete a workflow trigger")
    @DeleteMapping("/{triggerId}")
    public ResponseEntity<RestApiResponse<Void>> deleteTrigger(
            @Parameter(description = "Trigger ID") @PathVariable UUID triggerId) {
        log.info("Request to delete workflow trigger with ID: {}", triggerId);

        triggerService.deleteTrigger(triggerId);

        return RestApiResponse.success("Workflow trigger deleted successfully");
    }

    @Operation(summary = "Enable a workflow trigger")
    @PostMapping("/{triggerId}/enable")
    public ResponseEntity<RestApiResponse<WorkflowTriggerDto>> enableTrigger(
            @Parameter(description = "Trigger ID") @PathVariable UUID triggerId) {
        log.info("Request to enable workflow trigger with ID: {}", triggerId);

        WorkflowTriggerDto trigger = triggerService.enableTrigger(triggerId);

        return RestApiResponse.success(trigger, "Workflow trigger enabled successfully");
    }

    @Operation(summary = "Disable a workflow trigger")
    @PostMapping("/{triggerId}/disable")
    public ResponseEntity<RestApiResponse<WorkflowTriggerDto>> disableTrigger(
            @Parameter(description = "Trigger ID") @PathVariable UUID triggerId) {
        log.info("Request to disable workflow trigger with ID: {}", triggerId);

        WorkflowTriggerDto trigger = triggerService.disableTrigger(triggerId);

        return RestApiResponse.success(trigger, "Workflow trigger disabled successfully");
    }

    @Operation(summary = "Manually trigger a workflow")
    @PostMapping("/{triggerId}/execute")
    public ResponseEntity<RestApiResponse<Void>> executeTrigger(
            @Parameter(description = "Trigger ID") @PathVariable UUID triggerId,
            @Valid @RequestBody WorkflowRunnerRequest request
    ) {
        triggerService.executeTrigger(triggerId, request);

        return RestApiResponse.success("Workflow trigger executed successfully");
    }
}
