package org.phong.zenflow.workflow.subdomain.runner.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerResponse;
import org.phong.zenflow.workflow.subdomain.runner.service.WorkflowRunnerService;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/v1/workflows/{workflowId}/run")
public class WorkflowRunnerController {
    private WorkflowRunnerService workflowRunnerService;

    @PostMapping()
    public ResponseEntity<RestApiResponse<WorkflowRunnerResponse>> runWorkflow(@PathVariable String workflowId, @Valid @RequestBody WorkflowRunnerRequest request) {
        UUID workflowRunId = UUID.randomUUID();
        workflowRunnerService.runWorkflow(workflowRunId, TriggerType.MANUAL, UUID.fromString(workflowId), request);
        return RestApiResponse.accepted(new WorkflowRunnerResponse(
                workflowRunId,
                String.format("/api/v1/workflow-run/%s/status", workflowRunId)
        ));
    }
}
