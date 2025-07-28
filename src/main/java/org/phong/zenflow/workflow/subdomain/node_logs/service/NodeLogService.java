package org.phong.zenflow.workflow.subdomain.node_logs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowRetrySchedule;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.LogEntry;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.NodeLogStatus;
import org.phong.zenflow.workflow.subdomain.node_logs.exception.NodeLogException;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.mapstruct.NodeLogMapper;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.repository.NodeLogRepository;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.workflow_run.exception.WorkflowRunException;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.repository.WorkflowRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NodeLogService {

    private final NodeLogRepository nodeLogRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final NodeLogMapper nodeLogMapper;
    private final WorkflowRetrySchedule workflowRetrySchedule;
    private final ObjectMapper objectMapper;

    /**
     * Create a new node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_CREATE, targetIdExpression = "returnObject.id")
    public NodeLogDto createNodeLog(CreateNodeLogRequest request) {
        WorkflowRun workflowRun = workflowRunRepository.findById(request.workflowRunId())
                .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found with id: " + request.workflowRunId()));

        NodeLog nodeLog = nodeLogMapper.toEntity(request);
        nodeLog.setWorkflowRun(workflowRun);
        nodeLog.setStartedAt(OffsetDateTime.now());
        NodeLog savedNodeLog = nodeLogRepository.save(nodeLog);

        return nodeLogMapper.toDto(savedNodeLog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startNode(UUID workflowRunId, String nodeKey) {
        WorkflowRun run = workflowRunRepository.findById(workflowRunId)
                .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found"));

        NodeLog nodeLog = new NodeLog();
        nodeLog.setWorkflowRun(run);
        nodeLog.setNodeKey(nodeKey);
        nodeLog.setStatus(NodeLogStatus.RUNNING);
        nodeLog.setStartedAt(OffsetDateTime.now());
        nodeLog.setAttempts(1);

        nodeLogMapper.toDto(nodeLogRepository.save(nodeLog));
    }

    //TODO: make realtime log by insert log per step
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeNode(UUID workflowRunId, String nodeKey, NodeLogStatus status, String error,
                                   Map<String, Object> output, List<LogEntry> logs) {
        NodeLog nodeLog = nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeLogException("NodeLog not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        nodeLog.setStatus(status);
        nodeLog.setLogs(logs);
        if (error != null) {
            nodeLog.setError(error);
        }
        if (output != null) {
            nodeLog.setOutput(output);
        }
        if (nodeLog.getEndedAt() == null) {
            nodeLog.setEndedAt(OffsetDateTime.now());
        }

        nodeLogMapper.toDto(nodeLogRepository.save(nodeLog));
    }


    @Transactional
    public void waitNode(UUID workflowRunId, String nodeKey, NodeLogStatus status, List<LogEntry> logs, String reason) {
        NodeLog nodeLog = nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeLogException("NodeLog not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        nodeLog.setStatus(status);
        nodeLog.setLogs(logs);
        if (reason != null) {
            nodeLog.setError(reason);
        }

        nodeLogMapper.toDto(nodeLogRepository.save(nodeLog));
    }


    @Transactional
    public NodeLogDto retryNode(UUID workflowRunId, String nodeKey, List<LogEntry> logs) {
        NodeLog log = nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeLogException("NodeLog not found"));

        log.setAttempts(log.getAttempts() + 1);
        log.setStatus(NodeLogStatus.RETRYING);
        log.setStartedAt(OffsetDateTime.now());
        log.setLogs(logs);

        return nodeLogMapper.toDto(nodeLogRepository.save(log));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logValidationError(UUID workflowRunId, String nodeKey, ValidationResult validationResult) {
        NodeLog nodeLog = nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeLogException("NodeLog not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        String errorMessage;
        try {
            errorMessage = objectMapper.writeValueAsString(validationResult.getErrors());
        } catch (Exception e) {
            log.warn("Could not serialize validation errors to JSON", e);
            errorMessage = "Validation failed: " + validationResult.getErrors();
        }
        nodeLog.setStatus(NodeLogStatus.ERROR);
        nodeLog.setError(errorMessage);
        nodeLog.setEndedAt(OffsetDateTime.now());

        nodeLogRepository.save(nodeLog);
        log.error("Validation error in node {}: {}", nodeKey, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolveNodeLog(UUID workflowId, UUID workflowRunId, BaseWorkflowNode workingNode, ExecutionResult result) {
        switch (result.getStatus()) {
            case SUCCESS:
                log.debug("Plugin node executed successfully: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeLogStatus.SUCCESS, result.getError(), result.getOutput(), result.getLogs());
                break;
            case ERROR:
                log.error("Plugin node execution failed: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeLogStatus.ERROR, result.getError(), result.getOutput(), result.getLogs());
                break;
            case WAITING:
                log.debug("Plugin node execution skipped: {}", workingNode.getKey());
                waitNode(workflowRunId, workingNode.getKey(), NodeLogStatus.WAITING, result.getLogs(), result.getError());
                break;
            case RETRY:
                log.debug("Plugin node execution retrying: {}", workingNode.getKey());
                NodeLogDto retryNode = retryNode(workflowRunId, workingNode.getKey(), result.getLogs());
                workflowRetrySchedule.scheduleRetry(workflowId, workflowRunId, workingNode.getKey(), retryNode.attempts());
                break;
            case NEXT:
                log.debug("Plugin node execution next: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeLogStatus.NEXT, result.getError(), result.getOutput(), result.getLogs());
                break;
            case VALIDATION_ERROR:
                log.debug("Plugin node execution validation error: {}", workingNode.getKey());
                logValidationError(workflowRunId, workingNode.getKey(), result.getValidationResult());
                break;
            default:
                log.warn("Unknown status for plugin node execution: {}", result.getStatus());
        }
    }

    /**
     * Get a node log by ID
     */
    public NodeLogDto getNodeLogById(UUID id) {
        return nodeLogRepository.findById(id)
                .map(nodeLogMapper::toDto)
                .orElseThrow(() -> new NodeLogException("NodeLog not found with id: " + id));
    }

    /**
     * Get all node logs for a workflow run
     */
    public List<NodeLogDto> getNodeLogsByWorkflowRunId(UUID workflowRunId) {
        return nodeLogMapper.toDtoList(nodeLogRepository.findByWorkflowRunId(workflowRunId));
    }

    /**
     * Get all node logs (paginated)
     */
    public Page<NodeLogDto> getAllNodeLogs(Pageable pageable) {
        return nodeLogRepository.findAll(pageable)
                .map(nodeLogMapper::toDto);
    }

    /**
     * Update a node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_UPDATE, targetIdExpression = "#id")
    public NodeLogDto updateNodeLog(UUID id, UpdateNodeLogRequest request) {
        NodeLog nodeLog = nodeLogRepository.findById(id)
                .orElseThrow(() -> new NodeLogException("NodeLog not found with id: " + id));

        nodeLog = nodeLogMapper.partialUpdate(request, nodeLog);

        // If status indicates completion, set endedAt if not already set
        if ((request.status().equals("SUCCESS") || request.status().equals("ERROR")) && nodeLog.getEndedAt() == null) {
            nodeLog.setEndedAt(OffsetDateTime.now());
        }

        nodeLogRepository.save(nodeLog);

        return nodeLogMapper.toDto(nodeLog);
    }

    /**
     * Delete a node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_DELETE, targetIdExpression = "#id")
    public void deleteNodeLog(UUID id) {
        if (!nodeLogRepository.existsById(id)) {
            throw new NodeLogException("NodeLog not found with id: " + id);
        }
        nodeLogRepository.deleteById(id);
    }
}
