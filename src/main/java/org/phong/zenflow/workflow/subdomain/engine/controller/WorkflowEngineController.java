package org.phong.zenflow.workflow.subdomain.engine.controller;

import lombok.AllArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.engine.dto.WorkFlowEngineResponse;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflow/engine")
@AllArgsConstructor
public class WorkflowEngineController {
    private final WorkflowEngineService workflowEngineService;

    @PostMapping("/start/{workflowId}")
    public ResponseEntity<RestApiResponse<WorkFlowEngineResponse>> startWorkflow(@PathVariable String workflowId) {
        workflowEngineService.runWorkflow(UUID.fromString(workflowId));
        return RestApiResponse.accepted(new WorkFlowEngineResponse(
                UUID.randomUUID(),
                "/workflows/runs/abc-123/status"
        ));
    }
}
