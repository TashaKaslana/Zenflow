package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.runner.dto.WorkflowRunnerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.CreateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.UpdateWorkflowTriggerRequest;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerDto;
import org.phong.zenflow.workflow.subdomain.trigger.dto.WorkflowTriggerEvent;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;
import org.phong.zenflow.workflow.subdomain.trigger.exception.WorkflowTriggerException;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.mapstruct.WorkflowTriggerMapper;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.quartz.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkflowTriggerService {

    private final WorkflowTriggerRepository triggerRepository;
    private final WorkflowTriggerMapper triggerMapper;
    private final ApplicationEventPublisher publisher;
    private final TriggerRegistry triggerRegistry;
    private final TriggerOrchestrator triggerOrchestrator;

    @Transactional
    public WorkflowTriggerDto createTrigger(CreateWorkflowTriggerRequest request) {
        log.info("Creating workflow trigger for workflow ID: {}", request.getWorkflowId());

        WorkflowTrigger trigger = triggerMapper.toEntity(request);
        trigger = triggerRepository.save(trigger);

        triggerOrchestrator.start(trigger);

        log.info("Created workflow trigger with ID: {}", trigger.getId());
        return triggerMapper.toDto(trigger);
    }

    public void synchronizeTrigger(UUID workflowId, WorkflowDefinition wf) {
        Set<UUID> pluginNodeIds = wf.nodes().getPluginNodeIds();
        Set<String> triggerNodeIdSet = triggerRegistry.getAllTriggerKeys();
        Map<String, BaseWorkflowNode> nodeMap = wf.nodes().getNodeMapGroupByNodeId();

        // Find nodes in the workflow that match registered trigger types
        List<UUID> triggerNodeIds = pluginNodeIds.stream()
                .filter(nodeId -> {
                    BaseWorkflowNode node = nodeMap.get(nodeId.toString());
                    if (node == null) return false;

                    // Check if this node's type matches any registered trigger key
                    String nodeType = node.getPluginNode().getNodeId().toString();
                    return triggerNodeIdSet.contains(nodeType);
                })
                .toList();

        // Always upsert triggers - this ensures config is always up to date
        upsertTriggersForNodes(workflowId, triggerNodeIds, nodeMap);

        // Optional: Clean up triggers for nodes that no longer exist
        cleanupObsoleteTriggersOptional(workflowId, triggerNodeIds);
    }

    /**
     * Upsert triggers for the given nodes - always updates configuration
     */
    private void upsertTriggersForNodes(UUID workflowId, List<UUID> triggerNodeIds, Map<String, BaseWorkflowNode> nodeMap) {
        triggerNodeIds.forEach(nodeId -> {
            BaseWorkflowNode node = nodeMap.get(nodeId.toString());

            if (node != null) {
                Optional<TriggerType> triggerTypeOpt = triggerRegistry.getTriggerType(nodeId.toString());

                if (triggerTypeOpt.isPresent()) {
                    TriggerType triggerType = triggerTypeOpt.get();

                    // Check if trigger already exists
                    Optional<WorkflowTrigger> existingTrigger = triggerRepository
                            .findByWorkflowIdAndTriggerExecutorId(workflowId, nodeId);

                    if (existingTrigger.isPresent()) {
                        // Update existing trigger with new configuration
                        updateExistingTrigger(existingTrigger.get(), node, triggerType);
                    } else {
                        // Create new trigger
                        createNewTrigger(workflowId, nodeId, node, triggerType);
                    }

                    log.debug("Synchronized trigger for node ID: {} (type: {})", nodeId, triggerType.getType());
                } else {
                    log.warn("Could not determine trigger type for node {}", nodeId);
                }
            }
        });
    }

    /**
     * Update existing trigger with new configuration
     */
    private void updateExistingTrigger(WorkflowTrigger trigger, BaseWorkflowNode node, TriggerType triggerType) {
        Map<String, Object> newConfig = node.getConfig().input();
        TriggerType oldType = trigger.getType();

        // Always update the config - no comparison needed, just overwrite
        trigger.setConfig(newConfig);
        trigger.setType(triggerType);

        triggerRepository.save(trigger);

        // Manage schedule registration
        manageTriggerRunning(oldType, trigger);

        log.info("Updated existing trigger for node ID: {} with new configuration", trigger.getTriggerExecutorId());
    }

    /**
     * Create new trigger for node
     */
    private void createNewTrigger(UUID workflowId, UUID nodeId, BaseWorkflowNode node, TriggerType triggerType) {
        CreateWorkflowTriggerRequest request = new CreateWorkflowTriggerRequest();
        request.setWorkflowId(workflowId);
        request.setType(triggerType);
        request.setConfig(node.getConfig().input());
        request.setEnabled(true);
        request.setTriggerExecutorId(nodeId);

        createTrigger(request);
        log.info("Created new trigger for node ID: {} (type: {})", nodeId, triggerType.getType());
    }

    /**
     * Optional: Clean up triggers for nodes that no longer exist in the workflow
     * This prevents orphaned triggers from accumulating
     */
    private void cleanupObsoleteTriggersOptional(UUID workflowId, List<UUID> currentTriggerNodeIds) {
        if (currentTriggerNodeIds.isEmpty()) {
            return; // Skip cleanup if no trigger nodes
        }

        // Find triggers that exist in DB but not in current workflow
        List<WorkflowTrigger> allTriggersForWorkflow = triggerRepository.findByWorkflowId(workflowId);

        allTriggersForWorkflow.stream()
                .filter(trigger -> trigger.getTriggerExecutorId() != null)
                .filter(trigger -> !currentTriggerNodeIds.contains(trigger.getTriggerExecutorId()))
                .forEach(trigger -> {
                    log.info("Cleaning up obsolete trigger for node ID: {}", trigger.getTriggerExecutorId());
                    deleteTrigger(trigger.getId());
                });
    }

    public WorkflowTriggerDto getTriggerById(UUID triggerId) {
        log.debug("Retrieving workflow trigger with ID: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        return triggerMapper.toDto(trigger);
    }

    public List<WorkflowTriggerDto> getTriggersByWorkflowId(UUID workflowId) {
        log.debug("Retrieving workflow triggers for workflow ID: {}", workflowId);

        return triggerRepository.findByWorkflowId(workflowId)
                .stream()
                .map(triggerMapper::toDto)
                .collect(Collectors.toList());
    }

    public Page<WorkflowTriggerDto> getAllTriggers(Pageable pageable) {
        log.debug("Retrieving all workflow triggers with pagination");

        return triggerRepository.findAll(pageable)
                .map(triggerMapper::toDto);
    }

    @Transactional
    public WorkflowTriggerDto updateTrigger(UUID triggerId, UpdateWorkflowTriggerRequest request) {
        log.info("Updating workflow trigger with ID: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));


        triggerMapper.updateEntity(request, trigger);
        trigger = triggerRepository.save(trigger);

        manageTriggerRunning(request.getType(), trigger);

        log.info("Updated workflow trigger with ID: {}", triggerId);
        return triggerMapper.toDto(trigger);
    }

    private void manageTriggerRunning(TriggerType oldType, WorkflowTrigger trigger) {
        // Handle schedule trigger changes
        boolean typeChanged = oldType != null && oldType != trigger.getType();
        if (!typeChanged) {
            return; // No type change, no action needed
        }

        triggerOrchestrator.stop(trigger);
        if (trigger.getEnabled()) {
            triggerOrchestrator.start(trigger);
        }
    }

    @Transactional
    public void deleteTrigger(UUID triggerId) {
        log.info("Deleting workflow trigger with ID: {}", triggerId);
        WorkflowTrigger trigger = triggerRepository.findById(triggerId).orElseThrow(
                () -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString())
        );

        triggerRepository.deleteById(triggerId);

        triggerOrchestrator.stop(trigger);
        log.info("Deleted workflow trigger with ID: {}", triggerId);
    }

    @Transactional
    public WorkflowTriggerDto enableTrigger(UUID triggerId) {
        log.info("Enabling workflow trigger with ID: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        trigger.setEnabled(true);
        trigger = triggerRepository.save(trigger);

        //start running the trigger
        triggerOrchestrator.start(trigger);

        return triggerMapper.toDto(trigger);
    }

    @Transactional
    public WorkflowTriggerDto disableTrigger(UUID triggerId) {
        log.info("Disabling workflow trigger with ID: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        trigger.setEnabled(false);
        trigger = triggerRepository.save(trigger);

        triggerOrchestrator.stop(trigger);

        return triggerMapper.toDto(trigger);
    }

    @Transactional
    public WorkflowTrigger markTriggered(UUID triggerId) {
        log.debug("Marking trigger as triggered: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        trigger.setLastTriggeredAt(Instant.now());
        return triggerRepository.save(trigger);
    }

    @Transactional
    public UUID executeTrigger(UUID triggerId, WorkflowRunnerRequest request) {
        UUID workflowRunId = UUID.randomUUID();
        log.info("Execute workflow trigger with ID: {}", triggerId);
        WorkflowTrigger trigger = markTriggered(triggerId);
        publisher.publishEvent(new WorkflowTriggerEvent(
                workflowRunId,
                trigger.getType(),
                trigger.getTriggerExecutorId(),
                trigger.getWorkflowId(),
                request
        ));
        return workflowRunId;
    }
}
