package org.phong.zenflow.workflow.subdomain.node_execution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.engine.service.WorkflowRetrySchedule;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.CreateNodeExecutionRequest;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.NodeExecutionDto;
import org.phong.zenflow.workflow.subdomain.node_execution.dto.UpdateNodeExecutionRequest;
import org.phong.zenflow.workflow.subdomain.node_execution.enums.NodeExecutionStatus;
import org.phong.zenflow.workflow.subdomain.node_execution.exception.NodeExecutionException;
import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.mapstruct.NodeExecutionMapper;
import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.entity.NodeExecution;
import org.phong.zenflow.workflow.subdomain.node_execution.infrastructure.persistence.repository.NodeExecutionRepository;
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
public class NodeExecutionService {
    private final NodeExecutionRepository nodeExecutionRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final NodeExecutionMapper mapper;
    private final WorkflowRetrySchedule workflowRetrySchedule;
    private final ObjectMapper objectMapper;

    /**
     * Create a new node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_CREATE, targetIdExpression = "returnObject.id")
    public NodeExecutionDto createNodeExecution(CreateNodeExecutionRequest request) {
        WorkflowRun workflowRun = workflowRunRepository.findById(request.workflowRunId())
                .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found with id: " + request.workflowRunId()));

        NodeExecution nodeExecution = mapper.toEntity(request);
        nodeExecution.setWorkflowRun(workflowRun);
        nodeExecution.setStartedAt(OffsetDateTime.now());
        NodeExecution savedNodeExecution = nodeExecutionRepository.save(nodeExecution);

        return mapper.toDto(savedNodeExecution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startNode(UUID workflowRunId, String nodeKey) {
        WorkflowRun run = workflowRunRepository.findById(workflowRunId)
                .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found"));

        NodeExecution nodeExecution = new NodeExecution();
        nodeExecution.setWorkflowRun(run);
        nodeExecution.setNodeKey(nodeKey);
        nodeExecution.setStatus(NodeExecutionStatus.RUNNING);
        nodeExecution.setStartedAt(OffsetDateTime.now());
        nodeExecution.setAttempts(1);

        mapper.toDto(nodeExecutionRepository.save(nodeExecution));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeNode(UUID workflowRunId, String nodeKey, NodeExecutionStatus status, String error,
                             Map<String, Object> output) {
        NodeExecution nodeExecution = nodeExecutionRepository.findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeExecutionException("NodeExecution not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        nodeExecution.setStatus(status);
        if (error != null) {
            nodeExecution.setError(error);
        }
        if (output != null) {
            nodeExecution.setOutput(output);
        }
        if (nodeExecution.getEndedAt() == null) {
            nodeExecution.setEndedAt(OffsetDateTime.now());
        }

        mapper.toDto(nodeExecutionRepository.save(nodeExecution));
    }


    @Transactional
    public void waitNode(UUID workflowRunId, String nodeKey, NodeExecutionStatus status, String reason) {
        NodeExecution nodeExecution = nodeExecutionRepository.findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeExecutionException("NodeExecution not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        nodeExecution.setStatus(status);
        if (reason != null) {
            nodeExecution.setError(reason);
        }

        mapper.toDto(nodeExecutionRepository.save(nodeExecution));
    }


    @Transactional
    public NodeExecutionDto retryNode(UUID workflowRunId, String nodeKey) {
        NodeExecution log = nodeExecutionRepository.findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeExecutionException("NodeExecution not found"));

        log.setAttempts(log.getAttempts() + 1);
        log.setStatus(NodeExecutionStatus.RETRYING);
        log.setStartedAt(OffsetDateTime.now());

        return mapper.toDto(nodeExecutionRepository.save(log));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logValidationError(UUID workflowRunId, String nodeKey, ValidationResult validationResult) {
        NodeExecution nodeExecution = nodeExecutionRepository.findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(workflowRunId, nodeKey)
                .orElseThrow(() -> new NodeExecutionException("NodeExecution not found for workflowRunId: " + workflowRunId + " and nodeKey: " + nodeKey));

        String errorMessage;
        try {
            errorMessage = objectMapper.writeValueAsString(validationResult.getErrors());
        } catch (Exception e) {
            log.warn("Could not serialize validation errors to JSON", e);
            errorMessage = "Validation failed: " + validationResult.getErrors();
        }
        nodeExecution.setStatus(NodeExecutionStatus.ERROR);
        nodeExecution.setError(errorMessage);
        nodeExecution.setEndedAt(OffsetDateTime.now());

        nodeExecutionRepository.save(nodeExecution);
        log.error("Validation error in node {}: {}", nodeKey, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolveNodeExecution(UUID workflowId,
                                     UUID workflowRunId,
                                     BaseWorkflowNode workingNode,
                                     ExecutionResult result,
                                     String callbackUrl) {
        switch (result.getStatus()) {
            case SUCCESS:
                log.debug("Plugin node executed successfully: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.SUCCESS, result.getError(), result.getOutput());
                break;
            case ERROR:
                // Implement intelligent retry logic for errors
                if (shouldRetryOnError(workflowRunId, workingNode.getKey())) {
                    log.warn("Plugin node execution failed, attempting retry: {} (attempt: {})",
                            workingNode.getKey(), getCurrentAttempt(workflowRunId, workingNode.getKey()) + 1);
                    NodeExecutionDto retryNode = retryNode(workflowRunId, workingNode.getKey());
                    workflowRetrySchedule.scheduleRetry(
                            workflowId,
                            workflowRunId,
                            workingNode.getKey(),
                            retryNode.attempts(),
                            callbackUrl
                    );
                } else {
                    log.error("Plugin node execution failed after {} attempts, marking as error: {}",
                            WorkflowRetrySchedule.MAX_RETRY_ATTEMPTS, workingNode.getKey());
                    completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.ERROR, result.getError(), result.getOutput());
                }
                break;
            case WAITING:
                log.debug("Plugin node execution skipped: {}", workingNode.getKey());
                waitNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.WAITING, result.getError());
                break;
            case RETRY:
                log.debug("Plugin node execution retrying: {}", workingNode.getKey());
                NodeExecutionDto retryNode = retryNode(workflowRunId, workingNode.getKey());
                workflowRetrySchedule.scheduleRetry(
                        workflowId,
                        workflowRunId,
                        workingNode.getKey(),
                        retryNode.attempts(),
                        callbackUrl
                );
                break;
            case NEXT:
                log.debug("Plugin node execution next: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.NEXT, result.getError(), result.getOutput());
                break;
            case VALIDATION_ERROR:
                log.debug("Plugin node execution validation error: {}", workingNode.getKey());
                logValidationError(workflowRunId, workingNode.getKey(), result.getValidationResult());
                break;
            case LOOP_NEXT:
                log.debug("Plugin node execution loop next: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.LOOP_NEXT, result.getError(), result.getOutput());
                break;
            case LOOP_END:
                log.debug("Plugin node execution loop end: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.LOOP_END
                        , result.getError(), result.getOutput());
                break;
            case LOOP_CONTINUE:
                log.debug("Plugin node execution loop continue: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.LOOP_NEXT, result.getError(), result.getOutput());
                break;
            case LOOP_BREAK:
                log.debug("Plugin node execution loop break: {}", workingNode.getKey());
                completeNode(workflowRunId, workingNode.getKey(), NodeExecutionStatus.LOOP_END, result.getError(), result.getOutput());
                break;
            default:
                log.warn("Unknown status for plugin node execution: {}", result.getStatus());
        }
    }

    /**
     * Get a node log by ID
     */
    public NodeExecutionDto getNodeExecutionById(UUID id) {
        return nodeExecutionRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new NodeExecutionException("NodeExecution not found with id: " + id));
    }

    /**
     * Get all node logs for a workflow run
     */
    public List<NodeExecutionDto> getNodeExecutionsByWorkflowRunId(UUID workflowRunId) {
        return mapper.toDtoList(nodeExecutionRepository.findByWorkflowRunId(workflowRunId));
    }

    /**
     * Get all node logs (paginated)
     */
    public Page<NodeExecutionDto> getAllNodeExecutions(Pageable pageable) {
        return nodeExecutionRepository.findAll(pageable)
                .map(mapper::toDto);
    }

    /**
     * Update a node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_UPDATE, targetIdExpression = "#id")
    public NodeExecutionDto updateNodeExecution(UUID id, UpdateNodeExecutionRequest request) {
        NodeExecution nodeExecution = nodeExecutionRepository.findById(id)
                .orElseThrow(() -> new NodeExecutionException("NodeExecution not found with id: " + id));

        nodeExecution = mapper.partialUpdate(request, nodeExecution);

        // If status indicates completion, set endedAt if not already set
        if ((request.status().equals("SUCCESS") || request.status().equals("ERROR")) && nodeExecution.getEndedAt() == null) {
            nodeExecution.setEndedAt(OffsetDateTime.now());
        }

        nodeExecutionRepository.save(nodeExecution);

        return mapper.toDto(nodeExecution);
    }

    /**
     * Delete a node log
     */
    @Transactional
    @AuditLog(action = AuditAction.NODE_LOG_DELETE, targetIdExpression = "#id")
    public void deleteNodeExecution(UUID id) {
        if (!nodeExecutionRepository.existsById(id)) {
            throw new NodeExecutionException("NodeExecution not found with id: " + id);
        }
        nodeExecutionRepository.deleteById(id);
    }

    /**
     * Check if a node should be retried based on current attempts and max retry policy
     */
    private boolean shouldRetryOnError(UUID workflowRunId, String nodeKey) {
        return nodeExecutionRepository.findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(workflowRunId, nodeKey)
                .map(nodeExecution -> nodeExecution.getAttempts() < WorkflowRetrySchedule.MAX_RETRY_ATTEMPTS)
                .orElse(true); // If no log found, allow the first attempt
    }

    /**
     * Get the current attempt number for a node
     */
    private int getCurrentAttempt(UUID workflowRunId, String nodeKey) {
        return nodeExecutionRepository.findTopByWorkflowRunIdAndNodeKeyOrderByStartedAtDesc(workflowRunId, nodeKey)
                .map(NodeExecution::getAttempts)
                .orElse(0);
    }
}
