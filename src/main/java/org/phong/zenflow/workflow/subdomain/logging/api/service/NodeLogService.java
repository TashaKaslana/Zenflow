package org.phong.zenflow.workflow.subdomain.logging.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.collector.GlobalLogCollector;
import org.phong.zenflow.workflow.subdomain.logging.api.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.logging.api.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.logging.api.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.logging.api.enums.LogLevel;
import org.phong.zenflow.workflow.subdomain.logging.api.infrastructure.mapstruct.NodeLogMapper;
import org.phong.zenflow.workflow.subdomain.logging.api.infrastructure.persistence.entity.NodeLog;
import org.phong.zenflow.workflow.subdomain.logging.api.infrastructure.persistence.repository.NodeLogRepository;
import org.phong.zenflow.workflow.subdomain.workflow_run.exception.WorkflowRunException;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.repository.WorkflowRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NodeLogService {
    private final NodeLogRepository nodeLogRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final NodeLogMapper mapper;
    private final GlobalLogCollector globalLogCollector; // Use GlobalLogCollector instead

    /**
     * Create a new node log entry using JPA (for single entries with validation)
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_CREATE, targetIdExpression = "returnObject.id")
    public NodeLogDto createNodeLog(CreateNodeLogRequest request) {
        WorkflowRun workflowRun = workflowRunRepository.findById(request.workflowRunId())
                .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found with id: " + request.workflowRunId()));

        NodeLog nodeLog = mapper.toEntity(request);
        nodeLog.setWorkflowRun(workflowRun);
        NodeLog savedNodeLog = nodeLogRepository.save(nodeLog);

        return mapper.toDto(savedNodeLog);
    }

    /**
     * High-volume batch save using GlobalLogCollector for maximum performance
     * Use this for workflow execution logging where you have many log entries
     */
    public void saveBatchNodeLogs(UUID workflowRunId, List<LogEntry> logEntries) {
        if (logEntries.isEmpty()) {
            return;
        }

        log.debug("Submitting {} log entries for high-volume batch processing", logEntries.size());
        globalLogCollector.accept(workflowRunId, logEntries);
    }

    /**
     * Quick log method for individual entries during workflow execution
     * For high-volume scenarios, consider batching these into LogEntry objects
     */
    public void logMessage(UUID workflowId, UUID workflowRunId, String nodeKey, LogLevel level, String message) {
        // For individual logging, convert to LogEntry and use GlobalLogCollector for consistency
        LogEntry entry = LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .nodeKey(nodeKey)
                .timestamp(OffsetDateTime.now().toInstant())
                .level(convertToLogEntryLevel(level))
                .message(message)
                .build();

        globalLogCollector.accept(workflowRunId, List.of(entry));
    }

    /**
     * Quick error logging - routes through high-performance GlobalLogCollector
     */
    public void logError(UUID workflowId, UUID workflowRunId, String nodeKey, String message,
                         String errorCode, String errorMessage) {
        LogEntry entry = LogEntry.builder()
                .workflowId(workflowId)
                .workflowRunId(workflowRunId)
                .nodeKey(nodeKey)
                .timestamp(OffsetDateTime.now().toInstant())
                .level(org.phong.zenflow.workflow.subdomain.logging.core.LogLevel.ERROR)
                .message(message)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();

        globalLogCollector.accept(workflowRunId, List.of(entry));
    }

    // Convert between enum types
    private org.phong.zenflow.workflow.subdomain.logging.core.LogLevel convertToLogEntryLevel(LogLevel level) {
        return org.phong.zenflow.workflow.subdomain.logging.core.LogLevel.valueOf(level.name());
    }

    /**
     * Get node log by ID
     */
    public Optional<NodeLogDto> getNodeLogById(UUID id) {
        return nodeLogRepository.findById(id).map(mapper::toDto);
    }

    /**
     * Get all node logs for a workflow run
     */
    public Page<NodeLogDto> getNodeLogsByWorkflowRun(UUID workflowRunId, Pageable pageable) {
        return nodeLogRepository.findByWorkflowRunId(workflowRunId, pageable)
                .map(mapper::toDto);
    }

    /**
     * Get node logs for specific node in workflow run
     */
    public Page<NodeLogDto> getNodeLogsByWorkflowRunAndNode(UUID workflowRunId, String nodeKey, Pageable pageable) {
        return nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey, pageable)
                .map(mapper::toDto);
    }

    /**
     * Get node logs by level
     */
    public Page<NodeLogDto> getNodeLogsByLevel(UUID workflowRunId, LogLevel level, Pageable pageable) {
        return nodeLogRepository.findByWorkflowRunIdAndLevel(workflowRunId, level, pageable)
                .map(mapper::toDto);
    }

    /**
     * Get logs within time range
     */
    public List<NodeLogDto> getNodeLogsByTimeRange(UUID workflowRunId, OffsetDateTime startTime, OffsetDateTime endTime) {
        return nodeLogRepository.findByWorkflowRunIdAndTimestampBetween(workflowRunId, startTime, endTime)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Get logs by correlation ID
     */
    public List<NodeLogDto> getNodeLogsByCorrelationId(String correlationId) {
        return nodeLogRepository.findByCorrelationId(correlationId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Count logs by level
     */
    public long countLogsByLevel(UUID workflowRunId, LogLevel level) {
        return nodeLogRepository.countByWorkflowRunIdAndLevel(workflowRunId, level);
    }

    /**
     * Update node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_UPDATE, targetIdExpression = "#id")
    public Optional<NodeLogDto> updateNodeLog(UUID id, UpdateNodeLogRequest request) {
        return nodeLogRepository.findById(id)
                .map(nodeLog -> {
                    mapper.updateEntity(request, nodeLog);
                    NodeLog savedNodeLog = nodeLogRepository.save(nodeLog);
                    return mapper.toDto(savedNodeLog);
                });
    }

    /**
     * Delete node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_DELETE, targetIdExpression = "#id")
    public boolean deleteNodeLog(UUID id) {
        if (nodeLogRepository.existsById(id)) {
            nodeLogRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Delete all logs for a workflow run
     */
    @Transactional
    public void deleteLogsByWorkflowRun(UUID workflowRunId) {
        nodeLogRepository.deleteByWorkflowRunId(workflowRunId);
        log.info("Deleted all logs for workflow run: {}", workflowRunId);
    }
}
