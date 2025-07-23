package org.phong.zenflow.workflow.subdomain.node_logs.controller;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.service.NodeLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/node-logs")
@RequiredArgsConstructor
public class NodeLogController {

    private final NodeLogService nodeLogService;

    @PostMapping
    public ResponseEntity<RestApiResponse<NodeLogDto>> createNodeLog(@Valid @RequestBody CreateNodeLogRequest request) {
        NodeLogDto nodeLog = nodeLogService.createNodeLog(request);
        return RestApiResponse.created(nodeLog, "Node log created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<NodeLogDto>> getNodeLogById(@PathVariable UUID id) {
        NodeLogDto nodeLog = nodeLogService.getNodeLogById(id);
        return RestApiResponse.success(nodeLog, "Node log retrieved successfully");
    }

    @GetMapping("/workflow-run/{workflowRunId}")
    public ResponseEntity<RestApiResponse<List<NodeLogDto>>> getNodeLogsByWorkflowRunId(@PathVariable UUID workflowRunId) {
        List<NodeLogDto> nodeLogs = nodeLogService.getNodeLogsByWorkflowRunId(workflowRunId);
        return RestApiResponse.success(nodeLogs, "Node logs for workflow run retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<NodeLogDto>>> getAllNodeLogs(Pageable pageable) {
        Page<NodeLogDto> nodeLogs = nodeLogService.getAllNodeLogs(pageable);
        return RestApiResponse.success(nodeLogs, "Node logs retrieved successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<NodeLogDto>> updateNodeLog(@PathVariable UUID id, @Valid @RequestBody UpdateNodeLogRequest request) {
        NodeLogDto nodeLog = nodeLogService.updateNodeLog(id, request);
        return RestApiResponse.success(nodeLog, "Node log updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteNodeLog(@PathVariable UUID id) {
        nodeLogService.deleteNodeLog(id);
        return RestApiResponse.noContent();
    }
}
