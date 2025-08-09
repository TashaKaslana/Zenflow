package org.phong.zenflow.workflow.subdomain.workflow_version.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.workflow_version.dto.WorkflowVersionDto;
import org.phong.zenflow.workflow.subdomain.workflow_version.service.WorkflowVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workflows/{workflowId}/versions")
@RequiredArgsConstructor
public class WorkflowVersionController {

    private final WorkflowVersionService workflowVersionService;

    @GetMapping
    public ResponseEntity<RestApiResponse<List<WorkflowVersionDto>>> getVersions(@PathVariable UUID workflowId) {
        List<WorkflowVersionDto> versions = workflowVersionService.getVersions(workflowId);
        return RestApiResponse.success(versions, "Workflow versions retrieved successfully");
    }

    @GetMapping("/{version}")
    public ResponseEntity<RestApiResponse<WorkflowVersionDto>> getVersion(
            @PathVariable UUID workflowId,
            @PathVariable Integer version) {
        WorkflowVersionDto versionDto = workflowVersionService.getVersion(workflowId, version);
        return RestApiResponse.success(versionDto, "Workflow version retrieved successfully");
    }

    @DeleteMapping("/{version}")
    public ResponseEntity<RestApiResponse<Void>> deleteVersion(
            @PathVariable UUID workflowId,
            @PathVariable Integer version) {
        workflowVersionService.deleteVersion(workflowId, version);
        return RestApiResponse.noContent();
    }

    @DeleteMapping
    public ResponseEntity<RestApiResponse<Void>> deleteAllVersions(@PathVariable UUID workflowId) {
        workflowVersionService.deleteAllVersions(workflowId);
        return RestApiResponse.noContent();
    }
}

