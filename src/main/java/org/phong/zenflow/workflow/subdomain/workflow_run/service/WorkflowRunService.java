package org.phong.zenflow.workflow.subdomain.workflow_run.service;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.workflow.exception.WorkflowException;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.CreateWorkflowRunRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.UpdateWorkflowRunRequest;
import org.phong.zenflow.workflow.subdomain.workflow_run.dto.WorkflowRunDto;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.workflow_run.enums.WorkflowStatus;
import org.phong.zenflow.workflow.subdomain.workflow_run.exception.WorkflowRunException;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.mapstruct.WorkflowRunMapper;
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
public class WorkflowRunService {

    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowRunMapper workflowRunMapper;

    /**
     * Create a new workflow run
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_EXECUTE, targetIdExpression = "returnObject.id")
    public WorkflowRunDto createWorkflowRun(CreateWorkflowRunRequest request) {
        // Validate workflow exists
        Workflow workflow = workflowRepository.findById(request.workflowId())
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + request.workflowId()));

        WorkflowRun workflowRun = workflowRunMapper.toEntity(request);
        workflowRun.setWorkflow(workflow);
        workflowRun.setStartedAt(OffsetDateTime.now());

        // Set endedAt if status is completed
        if (request.status() == WorkflowStatus.SUCCESS || request.status() == WorkflowStatus.ERROR) {
            workflowRun.setEndedAt(OffsetDateTime.now());
        }

        WorkflowRun savedWorkflowRun = workflowRunRepository.save(workflowRun);
        return workflowRunMapper.toDto(savedWorkflowRun);
    }

    /**
     * Start a new workflow run
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_EXECUTE, targetIdExpression = "returnObject.id")
    public WorkflowRunDto startWorkflowRun(UUID workflowRunIdParam, UUID workflowId, TriggerType triggerType) {
        UUID workflowRunId = workflowRunIdParam != null ? workflowRunIdParam : UUID.randomUUID();
        CreateWorkflowRunRequest request = new CreateWorkflowRunRequest(
                workflowRunId,
                workflowId,
                WorkflowStatus.RUNNING,
                null,
                triggerType
        );
        return createWorkflowRun(request);
    }

    @Transactional
    public void handleWorkflowError(UUID workflowId, Exception e) {
        // Log the error and create a workflow run with error status
        String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
        UpdateWorkflowRunRequest request = new UpdateWorkflowRunRequest(
                WorkflowStatus.ERROR,
                errorMessage,
                null,
                OffsetDateTime.now()
        );
        updateWorkflowRun(workflowId, request);
    }

    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#workflowId", description = "Complete workflow run")
    @Transactional
    public void completeWorkflowRun(UUID workflowId) {
        // Complete the workflow run with success status
        UpdateWorkflowRunRequest request = new UpdateWorkflowRunRequest(
                WorkflowStatus.SUCCESS,
                null,
                null,
                OffsetDateTime.now()
        );
        updateWorkflowRun(workflowId, request);
    }

    /**
     * Find workflow run by ID
     */
    public WorkflowRunDto findById(UUID id) {
        WorkflowRun workflowRun = workflowRunRepository.findById(id)
                .orElseThrow(() -> new WorkflowRunException("Workflow run not found with id: " + id));
        return workflowRunMapper.toDto(workflowRun);
    }

    /**
     * Find all workflow runs
     */
    public List<WorkflowRunDto> findAll() {
        return workflowRunRepository.findAll()
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find workflow runs with pagination
     */
    public Page<WorkflowRunDto> findAll(Pageable pageable) {
        return workflowRunRepository.findAll(pageable)
                .map(workflowRunMapper::toDto);
    }

    /**
     * Find workflow runs by workflow ID
     */
    public List<WorkflowRunDto> findByWorkflowId(UUID workflowId) {
        // Validate workflow exists
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowException("Workflow not found with id: " + workflowId);
        }

        return workflowRunRepository.findByWorkflowId(workflowId)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find workflow runs by workflow ID with pagination
     */
    public Page<WorkflowRunDto> findByWorkflowId(UUID workflowId, Pageable pageable) {
        // Validate workflow exists
        if (!workflowRepository.existsById(workflowId)) {
            throw new WorkflowException("Workflow not found with id: " + workflowId);
        }

        return workflowRunRepository.findByWorkflowId(workflowId, pageable)
                .map(workflowRunMapper::toDto);
    }

    /**
     * Find workflow runs by status
     */
    public List<WorkflowRunDto> findByStatus(WorkflowStatus status) {
        return workflowRunRepository.findByStatus(status)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find workflow runs by workflow ID and status
     */
    public List<WorkflowRunDto> findByWorkflowIdAndStatus(UUID workflowId, WorkflowStatus status) {
        return workflowRunRepository.findByWorkflowIdAndStatus(workflowId, status)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find workflow runs by trigger type
     */
    public List<WorkflowRunDto> findByTriggerType(TriggerType triggerType) {
        return workflowRunRepository.findByTriggerType(triggerType)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find running workflow runs
     */
    public List<WorkflowRunDto> findRunningWorkflowRuns() {
        return workflowRunRepository.findRunningWorkflowRuns()
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find completed workflow runs
     */
    public List<WorkflowRunDto> findCompletedWorkflowRuns() {
        return workflowRunRepository.findCompletedWorkflowRuns()
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find latest workflow run for a workflow
     */
    public Optional<WorkflowRunDto> findLatestByWorkflowId(UUID workflowId) {
        return workflowRunRepository.findLatestByWorkflowId(workflowId)
                .map(workflowRunMapper::toDto);
    }

    /**
     * Find workflow runs within date range
     */
    public List<WorkflowRunDto> findByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        return workflowRunRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Find workflow runs by workflow ID within date range
     */
    public List<WorkflowRunDto> findByWorkflowIdAndDateRange(UUID workflowId, OffsetDateTime startDate, OffsetDateTime endDate) {
        return workflowRunRepository.findByWorkflowIdAndDateRange(workflowId, startDate, endDate)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Update workflow run
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#id")
    public WorkflowRunDto updateWorkflowRun(UUID id, UpdateWorkflowRunRequest request) {
        WorkflowRun existingWorkflowRun = workflowRunRepository.findById(id)
                .orElseThrow(() -> new WorkflowRunException("Workflow run not found with id: " + id));

        // If status is being updated to completed, set endedAt if not provided
        if (request.status() != null &&
            (request.status() == WorkflowStatus.SUCCESS || request.status() == WorkflowStatus.ERROR) &&
            request.endedAt() == null && existingWorkflowRun.getEndedAt() == null) {
            existingWorkflowRun.setEndedAt(OffsetDateTime.now());
        }

        WorkflowRun updated = workflowRunMapper.partialUpdate(request, existingWorkflowRun);
        WorkflowRun updatedWorkflowRun = workflowRunRepository.save(updated);
        return workflowRunMapper.toDto(updatedWorkflowRun);
    }

    /**
     * Complete workflow run with success status
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#id", description = "Complete workflow run successfully")
    public WorkflowRunDto completeWorkflowRunSuccess(UUID id) {
        UpdateWorkflowRunRequest request = new UpdateWorkflowRunRequest(
                WorkflowStatus.SUCCESS,
                null,
                null,
                OffsetDateTime.now()
        );
        return updateWorkflowRun(id, request);
    }

    /**
     * Complete workflow run with error status
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#id", description = "Complete workflow run with error")
    public WorkflowRunDto completeWorkflowRunError(UUID id, String errorMessage) {
        UpdateWorkflowRunRequest request = new UpdateWorkflowRunRequest(
                WorkflowStatus.ERROR,
                errorMessage,
                null,
                OffsetDateTime.now()
        );
        return updateWorkflowRun(id, request);
    }

    /**
     * Count workflow runs by workflow ID
     */
    public long countByWorkflowId(UUID workflowId) {
        return workflowRunRepository.countByWorkflowId(workflowId);
    }

    /**
     * Count workflow runs by workflow ID and status
     */
    public long countByWorkflowIdAndStatus(UUID workflowId, WorkflowStatus status) {
        return workflowRunRepository.countByWorkflowIdAndStatus(workflowId, status);
    }

    /**
     * Find long-running workflow runs
     */
    public List<WorkflowRunDto> findLongRunningWorkflowRuns(int hours) {
        OffsetDateTime cutoffTime = OffsetDateTime.now().minusHours(hours);
        return workflowRunRepository.findLongRunningWorkflowRuns(cutoffTime)
                .stream()
                .map(workflowRunMapper::toDto)
                .toList();
    }

    /**
     * Delete workflow run
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_DELETE, targetIdExpression = "#id", description = "Delete workflow run")
    public void deleteWorkflowRun(UUID id) {
        if (!workflowRunRepository.existsById(id)) {
            throw new WorkflowRunException("Workflow run not found with id: " + id);
        }
        workflowRunRepository.deleteById(id);
    }

    /**
     * Check if workflow run exists
     */
    public boolean existsById(UUID id) {
        return workflowRunRepository.existsById(id);
    }
}
