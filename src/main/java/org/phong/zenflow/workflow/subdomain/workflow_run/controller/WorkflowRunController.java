package org.phong.zenflow.workflow.subdomain.workflow_run.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.CreateWorkflowRunRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.UpdateWorkflowRunRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.WorkflowRunDto;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.WorkflowStatus;
import org.phong.zenflow.workflow.subdomain.workflow_run.service.WorkflowRunService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/workflow-runs")
@RequiredArgsConstructor
public class WorkflowRunController {

    private final WorkflowRunService workflowRunService;

    @PostMapping
    public ResponseEntity<RestApiResponse<WorkflowRunDto>> createWorkflowRun(@Valid @RequestBody CreateWorkflowRunRequest request) {
        WorkflowRunDto createdWorkflowRun = workflowRunService.createWorkflowRun(request);
        return RestApiResponse.created(createdWorkflowRun, "Workflow run created successfully");
    }

    @PostMapping("/start/{workflowId}")
    public ResponseEntity<RestApiResponse<WorkflowRunDto>> startWorkflowRun(
            @PathVariable UUID workflowId,
            @RequestParam(defaultValue = "MANUAL") TriggerType triggerType) {
        WorkflowRunDto workflowRun = workflowRunService.startWorkflowRun(null, workflowId, triggerType);
        return RestApiResponse.created(workflowRun, "Workflow run started successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<WorkflowRunDto>> getWorkflowRunById(@PathVariable UUID id) {
        WorkflowRunDto workflowRun = workflowRunService.findById(id);
        return RestApiResponse.success(workflowRun, "Workflow run retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getAllWorkflowRuns() {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findAll();
        return RestApiResponse.success(workflowRuns, "Workflow runs retrieved successfully");
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getAllWorkflowRunsPaginated(Pageable pageable) {
        Page<WorkflowRunDto> workflowRuns = workflowRunService.findAll(pageable);
        return RestApiResponse.success(workflowRuns, "Workflow runs retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByWorkflowId(@PathVariable UUID workflowId) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findByWorkflowId(workflowId);
        return RestApiResponse.success(workflowRuns, "Workflow runs retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}/paginated")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByWorkflowIdPaginated(
            @PathVariable UUID workflowId, Pageable pageable) {
        Page<WorkflowRunDto> workflowRuns = workflowRunService.findByWorkflowId(workflowId, pageable);
        return RestApiResponse.success(workflowRuns, "Workflow runs retrieved successfully");
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByStatus(@PathVariable WorkflowStatus status) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findByStatus(status);
        return RestApiResponse.success(workflowRuns, "Workflow runs by status retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}/status/{status}")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByWorkflowIdAndStatus(
            @PathVariable UUID workflowId, @PathVariable WorkflowStatus status) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findByWorkflowIdAndStatus(workflowId, status);
        return RestApiResponse.success(workflowRuns, "Workflow runs by workflow and status retrieved successfully");
    }

    @GetMapping("/trigger-type/{triggerType}")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByTriggerType(@PathVariable TriggerType triggerType) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findByTriggerType(triggerType);
        return RestApiResponse.success(workflowRuns, "Workflow runs by trigger type retrieved successfully");
    }

    @GetMapping("/running")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getRunningWorkflowRuns() {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findRunningWorkflowRuns();
        return RestApiResponse.success(workflowRuns, "Running workflow runs retrieved successfully");
    }

    @GetMapping("/completed")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getCompletedWorkflowRuns() {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findCompletedWorkflowRuns();
        return RestApiResponse.success(workflowRuns, "Completed workflow runs retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}/latest")
    public ResponseEntity<RestApiResponse<Optional<WorkflowRunDto>>> getLatestWorkflowRunByWorkflowId(@PathVariable UUID workflowId) {
        Optional<WorkflowRunDto> workflowRun = workflowRunService.findLatestByWorkflowId(workflowId);
        if (workflowRun.isPresent()) {
            return RestApiResponse.success(workflowRun, "Latest workflow run retrieved successfully");
        } else {
            return RestApiResponse.success(Optional.empty(), "No workflow runs found for this workflow");
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findByDateRange(startDate, endDate);
        return RestApiResponse.success(workflowRuns, "Workflow runs in date range retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}/date-range")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getWorkflowRunsByWorkflowIdAndDateRange(
            @PathVariable UUID workflowId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findByWorkflowIdAndDateRange(workflowId, startDate, endDate);
        return RestApiResponse.success(workflowRuns, "Workflow runs for workflow in date range retrieved successfully");
    }

    @GetMapping("/long-running")
    public ResponseEntity<RestApiResponse<List<WorkflowRunDto>>> getLongRunningWorkflowRuns(
            @RequestParam(defaultValue = "24") int hours) {
        List<WorkflowRunDto> workflowRuns = workflowRunService.findLongRunningWorkflowRuns(hours);
        return RestApiResponse.success(workflowRuns, "Long-running workflow runs retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}/count")
    public ResponseEntity<RestApiResponse<Long>> countWorkflowRunsByWorkflowId(@PathVariable UUID workflowId) {
        long count = workflowRunService.countByWorkflowId(workflowId);
        return RestApiResponse.success(count, "Workflow run count retrieved successfully");
    }

    @GetMapping("/workflow/{workflowId}/count/status/{status}")
    public ResponseEntity<RestApiResponse<Long>> countWorkflowRunsByWorkflowIdAndStatus(
            @PathVariable UUID workflowId, @PathVariable WorkflowStatus status) {
        long count = workflowRunService.countByWorkflowIdAndStatus(workflowId, status);
        return RestApiResponse.success(count, "Workflow run count by status retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<WorkflowRunDto>> updateWorkflowRun(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkflowRunRequest request) {
        WorkflowRunDto updatedWorkflowRun = workflowRunService.updateWorkflowRun(id, request);
        return RestApiResponse.success(updatedWorkflowRun, "Workflow run updated successfully");
    }

    @PatchMapping("/{id}/complete/success")
    public ResponseEntity<RestApiResponse<WorkflowRunDto>> completeWorkflowRunSuccess(@PathVariable UUID id) {
        WorkflowRunDto workflowRun = workflowRunService.completeWorkflowRunSuccess(id);
        return RestApiResponse.success(workflowRun, "Workflow run completed successfully");
    }

    @PatchMapping("/{id}/complete/error")
    public ResponseEntity<RestApiResponse<WorkflowRunDto>> completeWorkflowRunError(
            @PathVariable UUID id,
            @RequestParam String errorMessage) {
        WorkflowRunDto workflowRun = workflowRunService.completeWorkflowRunError(id, errorMessage);
        return RestApiResponse.success(workflowRun, "Workflow run completed with error");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteWorkflowRun(@PathVariable UUID id) {
        workflowRunService.deleteWorkflowRun(id);
        return RestApiResponse.noContent();
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<RestApiResponse<Boolean>> checkWorkflowRunExists(@PathVariable UUID id) {
        boolean exists = workflowRunService.existsById(id);
        return RestApiResponse.success(exists, "Workflow run existence checked");
    }
}
