package org.phong.zenflow.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.project.exception.ProjectNotFoundException;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.project.infrastructure.persistence.repository.ProjectRepository;
import org.phong.zenflow.workflow.dto.CreateWorkflowRequest;
import org.phong.zenflow.workflow.dto.UpdateWorkflowRequest;
import org.phong.zenflow.workflow.dto.UpsertWorkflowDefinition;
import org.phong.zenflow.workflow.dto.WorkflowDto;
import org.phong.zenflow.workflow.exception.WorkflowException;
import org.phong.zenflow.workflow.infrastructure.mapstruct.WorkflowMapper;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.services.WorkflowDefinitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WorkflowService {
    private final WorkflowRepository workflowRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowMapper workflowMapper;
    private final WorkflowDefinitionService definitionService;

    /**
     * Create a new workflow
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_CREATE, targetIdExpression = "returnObject.id")
    public WorkflowDto createWorkflow(CreateWorkflowRequest request) {
        // Validate project exists
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.projectId()));

        Workflow workflow = workflowMapper.toEntity(request);
        workflow.setProject(project);
        workflow.setIsActive(false); // Default to inactive

        Workflow savedWorkflow = workflowRepository.save(workflow);
        return workflowMapper.toDto(savedWorkflow);
    }

    /**
     * Create multiple workflows in bulk
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_CREATE)
    public List<WorkflowDto> createWorkflows(List<CreateWorkflowRequest> requests) {
        return requests.stream()
                .map(this::createWorkflow)
                .toList();
    }

    /**
     * Update workflow nodes. Accepts incoming nodes, validates them, and updates the workflow definition.
     *
     * @param workflowId ID of the workflow to update
     * @param incomingNodes List of nodes to add or update
     * @return Updated workflow definition
     */
    @Transactional
    public WorkflowDefinition upsertNodes(UUID workflowId, UpsertWorkflowDefinition incomingNodes) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + workflowId));

        WorkflowDefinition existWorkflowDef = workflow.getDefinition();

        if (existWorkflowDef == null) {
            existWorkflowDef = new WorkflowDefinition().init();
        }

        WorkflowDefinition newWorkflowDef = new WorkflowDefinition(incomingNodes.nodes(), incomingNodes.metadata());

        WorkflowDefinition upserted = definitionService.upsert(newWorkflowDef, existWorkflowDef);

        workflow.setDefinition(upserted);
        workflowRepository.save(workflow);

        return upserted;
    }

    /**
     * Remove a node from a workflow by its key
     *
     * @param workflowId ID of the workflow
     * @param keyToRemove Key of the node to remove
     * @return Updated workflow definition
     */
    @Transactional
    public WorkflowDefinition removeNode(UUID workflowId, String keyToRemove) {
        Workflow workflow = getWorkflow(workflowId);
        WorkflowDefinition definition = workflow.getDefinition();
        if (definition == null || definition.nodes() == null) {
            return new WorkflowDefinition();
        }

        WorkflowDefinition updatedDefinition = definitionService.removeNode(definition, keyToRemove);

        workflow.setDefinition(updatedDefinition);
        workflowRepository.save(workflow);
        log.debug("Workflow with ID: {} has been updated by removing node with key: {}", workflowId, keyToRemove);

        return updatedDefinition;
    }

    public WorkflowDefinition clearWorkflowDefinition(UUID workflowId) {
        Workflow workflow = getWorkflow(workflowId);
        WorkflowDefinition definition = workflow.getDefinition();
        if (definition == null || definition.nodes() == null) {
            return new WorkflowDefinition();
        }

        WorkflowDefinition clearedDefinition = definitionService.clearWorkflowDefinition(definition);

        workflow.setDefinition(clearedDefinition);
        workflowRepository.save(workflow);
        log.debug("Workflow with ID: {} has been updated by clearing all nodes", workflowId);

        return clearedDefinition;
    }

    public Workflow getWorkflow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow not found"));
    }

    /**
     * Find workflow by ID
     */
    public WorkflowDto findById(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + id));
        return workflowMapper.toDto(workflow);
    }

    /**
     * Find all workflows
     */
    public List<WorkflowDto> findAll() {
        return workflowRepository.findAll()
                .stream()
                .map(workflowMapper::toDto)
                .toList();
    }

    /**
     * Find workflows with pagination
     */
    public Page<WorkflowDto> findAll(Pageable pageable) {
        return workflowRepository.findAll(pageable)
                .map(workflowMapper::toDto);
    }

    /**
     * Find workflows by project ID
     */
    public List<WorkflowDto> findByProjectId(UUID projectId) {
        // Validate project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        return workflowRepository.findByProjectId(projectId)
                .stream()
                .map(workflowMapper::toDto)
                .toList();
    }

    /**
     * Find active workflows by project ID
     */
    public List<WorkflowDto> findActiveByProjectId(UUID projectId) {
        // Validate project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        return workflowRepository.findByProjectIdAndIsActive(projectId, true)
                .stream()
                .map(workflowMapper::toDto)
                .toList();
    }

    /**
     * Update workflow
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#id")
    public WorkflowDto updateWorkflow(UUID id, UpdateWorkflowRequest request) {
        Workflow existingWorkflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + id));

        Workflow updated = workflowMapper.partialUpdate(request, existingWorkflow);
        Workflow updatedWorkflow = workflowRepository.save(updated);
        return workflowMapper.toDto(updatedWorkflow);
    }

    /**
     * Activate workflow
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#id", description = "Activate workflow")
    public WorkflowDto activateWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + id));

        workflow.setIsActive(true);
        Workflow updatedWorkflow = workflowRepository.save(workflow);
        return workflowMapper.toDto(updatedWorkflow);
    }

    /**
     * Deactivate workflow
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_UPDATE, targetIdExpression = "#id", description = "Deactivate workflow")
    public WorkflowDto deactivateWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + id));

        workflow.setIsActive(false);
        Workflow updatedWorkflow = workflowRepository.save(workflow);
        return workflowMapper.toDto(updatedWorkflow);
    }

    /**
     * Soft delete workflow
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_DELETE, targetIdExpression = "#id")
    public void deleteWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new WorkflowException("Workflow not found with id: " + id));

        workflow.setDeletedAt(OffsetDateTime.now());
        workflow.setIsActive(false); // Deactivate when deleting
        workflowRepository.save(workflow);
    }

    /**
     * Hard delete workflow
     */
    @Transactional
    @AuditLog(action = AuditAction.WORKFLOW_DELETE, targetIdExpression = "#id", description = "Hard delete workflow")
    public void hardDeleteWorkflow(UUID id) {
        if (!workflowRepository.existsById(id)) {
            throw new WorkflowException("Workflow not found with id: " + id);
        }
        workflowRepository.deleteById(id);
    }

    /**
     * Check if the workflow exists
     */
    public boolean existsById(UUID id) {
        return workflowRepository.existsById(id);
    }

    /**
     * Count workflows by project ID
     */
    public long countByProjectId(UUID projectId) {
        return workflowRepository.countByProjectId(projectId);
    }

    /**
     * Count active workflows by project ID
     */
    public long countActiveByProjectId(UUID projectId) {
        return workflowRepository.countByProjectIdAndIsActive(projectId, true);
    }

    public Workflow getReferenceById(UUID id) {
        return workflowRepository.getReferenceById(id);
    }
}
