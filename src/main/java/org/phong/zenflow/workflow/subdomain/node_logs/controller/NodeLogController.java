package org.phong.zenflow.workflow.subdomain.node_logs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.LogLevel;
import org.phong.zenflow.workflow.subdomain.node_logs.service.NodeLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/node-logs")
@RequiredArgsConstructor
@Tag(name = "Node Logs", description = "Node logging operations")
public class NodeLogController {

    private final NodeLogService nodeLogService;

    @PostMapping
    @Operation(summary = "Create a new node log entry")
    public ResponseEntity<NodeLogDto> createNodeLog(@Valid @RequestBody CreateNodeLogRequest request) {
        NodeLogDto nodeLog = nodeLogService.createNodeLog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeLog);
    }

    @PostMapping("/quick-log")
    @Operation(summary = "Quick log message")
    public ResponseEntity<Void> quickLog(
            @RequestParam UUID workflowId,
            @RequestParam UUID workflowRunId,
            @RequestParam String nodeKey,
            @RequestParam LogLevel level,
            @RequestParam String message) {
        nodeLogService.logMessage(workflowId, workflowRunId, nodeKey, level, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/log-error")
    @Operation(summary = "Quick error log")
    public ResponseEntity<Void> logError(
            @RequestParam UUID workflowId,
            @RequestParam UUID workflowRunId,
            @RequestParam String nodeKey,
            @RequestParam String message,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMessage) {
        nodeLogService.logError(workflowId, workflowRunId, nodeKey, message, errorCode, errorMessage);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get node log by ID")
    public ResponseEntity<NodeLogDto> getNodeLogById(@PathVariable UUID id) {
        return nodeLogService.getNodeLogById(id)
                .map(nodeLog -> ResponseEntity.ok().body(nodeLog))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/workflow-run/{workflowRunId}")
    @Operation(summary = "Get all node logs for a workflow run")
    public ResponseEntity<Page<NodeLogDto>> getNodeLogsByWorkflowRun(
            @PathVariable UUID workflowRunId,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        Page<NodeLogDto> logs = nodeLogService.getNodeLogsByWorkflowRun(workflowRunId, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/workflow-run/{workflowRunId}/node/{nodeKey}")
    @Operation(summary = "Get node logs for specific node in workflow run")
    public ResponseEntity<Page<NodeLogDto>> getNodeLogsByWorkflowRunAndNode(
            @PathVariable UUID workflowRunId,
            @PathVariable String nodeKey,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        Page<NodeLogDto> logs = nodeLogService.getNodeLogsByWorkflowRunAndNode(workflowRunId, nodeKey, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/workflow-run/{workflowRunId}/level/{level}")
    @Operation(summary = "Get node logs by level for a workflow run")
    public ResponseEntity<Page<NodeLogDto>> getNodeLogsByLevel(
            @PathVariable UUID workflowRunId,
            @PathVariable LogLevel level,
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        Page<NodeLogDto> logs = nodeLogService.getNodeLogsByLevel(workflowRunId, level, pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/workflow-run/{workflowRunId}/time-range")
    @Operation(summary = "Get node logs within time range")
    public ResponseEntity<List<NodeLogDto>> getNodeLogsByTimeRange(
            @PathVariable UUID workflowRunId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime) {
        List<NodeLogDto> logs = nodeLogService.getNodeLogsByTimeRange(workflowRunId, startTime, endTime);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/correlation/{correlationId}")
    @Operation(summary = "Get node logs by correlation ID")
    public ResponseEntity<List<NodeLogDto>> getNodeLogsByCorrelationId(@PathVariable String correlationId) {
        List<NodeLogDto> logs = nodeLogService.getNodeLogsByCorrelationId(correlationId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/workflow-run/{workflowRunId}/count/{level}")
    @Operation(summary = "Count logs by level for a workflow run")
    public ResponseEntity<Long> countLogsByLevel(
            @PathVariable UUID workflowRunId,
            @PathVariable LogLevel level) {
        long count = nodeLogService.countLogsByLevel(workflowRunId, level);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update node log")
    public ResponseEntity<NodeLogDto> updateNodeLog(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNodeLogRequest request) {
        return nodeLogService.updateNodeLog(id, request)
                .map(nodeLog -> ResponseEntity.ok().body(nodeLog))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete node log")
    public ResponseEntity<Void> deleteNodeLog(@PathVariable UUID id) {
        boolean deleted = nodeLogService.deleteNodeLog(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/workflow-run/{workflowRunId}")
    @Operation(summary = "Delete all logs for a workflow run")
    public ResponseEntity<Void> deleteLogsByWorkflowRun(@PathVariable UUID workflowRunId) {
        nodeLogService.deleteLogsByWorkflowRun(workflowRunId);
        return ResponseEntity.noContent().build();
    }
}
