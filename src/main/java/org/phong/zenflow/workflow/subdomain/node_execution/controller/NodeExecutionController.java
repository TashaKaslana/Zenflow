package org.phong.zenflow.workflow.subdomain.node_execution.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.CreateNodeExecutionRequest;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.NodeExecutionDto;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.UpdateNodeExecutionRequest;
import org.phong.zenflow.workflow.subdomain.node_execution.service.NodeExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/node-executions")
@RequiredArgsConstructor
public class NodeExecutionController {
    private final NodeExecutionService nodeExecutionService;

    @PostMapping
    public ResponseEntity<RestApiResponse<NodeExecutionDto>> createNodeExecution(@Valid @RequestBody CreateNodeExecutionRequest request) {
        NodeExecutionDto nodeExecution = nodeExecutionService.createNodeExecution(request);
        return RestApiResponse.created(nodeExecution, "Node log created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<NodeExecutionDto>> getNodeExecutionById(@PathVariable UUID id) {
        NodeExecutionDto nodeExecution = nodeExecutionService.getNodeExecutionById(id);
        return RestApiResponse.success(nodeExecution, "Node log retrieved successfully");
    }

    @GetMapping("/workflow-run/{workflowRunId}")
    public ResponseEntity<RestApiResponse<List<NodeExecutionDto>>> getNodeExecutionsByWorkflowRunId(@PathVariable UUID workflowRunId) {
        List<NodeExecutionDto> nodeExecutions = nodeExecutionService.getNodeExecutionsByWorkflowRunId(workflowRunId);
        return RestApiResponse.success(nodeExecutions, "Node logs for workflow run retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<NodeExecutionDto>>> getAllNodeExecutions(Pageable pageable) {
        Page<NodeExecutionDto> nodeExecutions = nodeExecutionService.getAllNodeExecutions(pageable);
        return RestApiResponse.success(nodeExecutions, "Node logs retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<NodeExecutionDto>> updateNodeExecution(@PathVariable UUID id, @Valid @RequestBody UpdateNodeExecutionRequest request) {
        NodeExecutionDto nodeExecution = nodeExecutionService.updateNodeExecution(id, request);
        return RestApiResponse.success(nodeExecution, "Node log updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteNodeExecution(@PathVariable UUID id) {
        nodeExecutionService.deleteNodeExecution(id);
        return RestApiResponse.noContent();
    }
}
