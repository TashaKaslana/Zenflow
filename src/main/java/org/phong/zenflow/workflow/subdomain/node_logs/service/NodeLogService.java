package org.phong.zenflow.workflow.subdomain.node_logs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.CreateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.NodeLogDto;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.UpdateNodeLogRequest;
import org.phong.zenflow.workflow.subdomain.node_logs.enums.NodeLogStatus;
import org.phong.zenflow.workflow.subdomain.node_logs.exception.NodeLogException;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.mapstruct.NodeLogMapper;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog;
import org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.repository.NodeLogRepository;
import org.phong.zenflow.workflow.subdomain.workflow_run.exception.WorkflowRunException;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.repository.WorkflowRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

    @Transactional
    public NodeLogDto startNode(UUID workflowRunId, String nodeKey) {
        WorkflowRun run = workflowRunRepository.findById(workflowRunId)
                .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found"));

        NodeLog nodeLog = new NodeLog();
        nodeLog.setWorkflowRun(run);
        nodeLog.setNodeKey(nodeKey);
        nodeLog.setStatus(NodeLogStatus.RUNNING);
        nodeLog.setStartedAt(OffsetDateTime.now());
        nodeLog.setAttempts(1);

        return nodeLogMapper.toDto(nodeLogRepository.save(nodeLog));
    }

    @Transactional
    public NodeLogDto completeNode(UUID workflowRunId, String nodeKey, NodeLogStatus status, String error, Map<String, Object> output) {
        NodeLog nodeLog = nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeLogException("NodeLog not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        nodeLog.setStatus(status);
        if (error != null) {
            nodeLog.setError(error);
        }
        if (output != null) {
            nodeLog.setOutput(output);
        }
        if (nodeLog.getEndedAt() == null) {
            nodeLog.setEndedAt(OffsetDateTime.now());
        }

        return nodeLogMapper.toDto(nodeLogRepository.save(nodeLog));
    }

    @Transactional
    public NodeLogDto retryNode(UUID workflowRunId, String nodeKey) {
        NodeLog log = nodeLogRepository.findByWorkflowRunIdAndNodeKey(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeLogException("NodeLog not found"));

        log.setAttempts(log.getAttempts() + 1);
        log.setStatus(NodeLogStatus.WAITING);
        log.setStartedAt(OffsetDateTime.now());
        log.setEndedAt(null); // reset end time for retry

        return nodeLogMapper.toDto(nodeLogRepository.save(log));
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
