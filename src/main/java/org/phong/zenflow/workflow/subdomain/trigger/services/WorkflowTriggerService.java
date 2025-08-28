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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final WorkflowSchedulerService workflowSchedulerService;
    private final ApplicationEventPublisher publisher;
    private final TriggerRegistry triggerRegistry;

    @Transactional
    public WorkflowTriggerDto createTrigger(CreateWorkflowTriggerRequest request) {
        log.info("Creating workflow trigger for workflow ID: {}", request.getWorkflowId());

        // Validate trigger configuration based on type
        validateTriggerConfiguration(request);

        WorkflowTrigger trigger = triggerMapper.toEntity(request);
        trigger = triggerRepository.save(trigger);

        if (trigger.getType() == TriggerType.SCHEDULE) {
            workflowSchedulerService.registerSchedule(trigger);
        }

        log.info("Created workflow trigger with ID: {}", trigger.getId());
        return triggerMapper.toDto(trigger);
    }

    public void synchronizeTrigger(UUID workflowId, WorkflowDefinition wf) {
        Set<UUID> pluginNodeIds = wf.getPluginNodeIds();
        Set<String> triggerKeys = triggerRegistry.getAllTriggerKeys();
        Map<String, BaseWorkflowNode> nodeMap = wf.getNodeMap();

        // Find nodes in the workflow that match registered trigger types
        List<UUID> triggerNodeIds = pluginNodeIds.stream()
                .filter(nodeId -> {
                    BaseWorkflowNode node = nodeMap.get(nodeId.toString());
                    if (node == null) return false;

                    // Check if this node's type matches any registered trigger key
                    String nodeType = node.getType().getNodeType();
                    return triggerKeys.contains(nodeType);
                })
                .toList();

        List<WorkflowTrigger> existingTriggers = triggerRepository.findByWorkflowId(workflowId);

        triggerNodeIds.forEach(nodeId -> {
            BaseWorkflowNode node = nodeMap.get(nodeId.toString());

            if (node != null) {
                // Check if trigger already exists for this node
                boolean triggerExists = existingTriggers.stream()
                        .anyMatch(trigger ->
                            trigger.getConfig().containsKey("nodeId") &&
                            trigger.getConfig().get("nodeId").equals(nodeId.toString())
                        );

                if (!triggerExists) {
                    // Get the trigger type from the registry
                    String nodeType = node.getType().getNodeType();
                    Optional<TriggerType> triggerTypeOpt = triggerRegistry.getTriggerType(nodeType);

                    if (triggerTypeOpt.isPresent()) {
                        TriggerType triggerType = triggerTypeOpt.get();

                        CreateWorkflowTriggerRequest request = new CreateWorkflowTriggerRequest();
                        request.setWorkflowId(workflowId);
                        request.setType(triggerType);
                        request.setConfig(Map.of(
                            "nodeId", nodeId.toString(),
                            "nodeType", nodeType
                        ));
                        request.setEnabled(true);

                        createTrigger(request);
                        log.info("Synchronized and created {} trigger for node ID: {} (type: {})",
                                triggerType, nodeId, nodeType);
                    } else {
                        log.warn("Could not determine trigger type for node {} with type {}", nodeId, nodeType);
                    }
                }
            }
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

        // Validate updated configuration if type is changed
        if (request.getType() != null && request.getType() != trigger.getType()) {
            validateTriggerConfigurationForType(request.getType(), request.getConfig());
        }

        triggerMapper.updateEntity(request, trigger);
        trigger = triggerRepository.save(trigger);
        if (trigger.getType() == TriggerType.SCHEDULE && request.getType() != TriggerType.SCHEDULE) {
            workflowSchedulerService.removeSchedule(triggerId);
        }

        log.info("Updated workflow trigger with ID: {}", triggerId);
        return triggerMapper.toDto(trigger);
    }

    @Transactional
    public void deleteTrigger(UUID triggerId) {
        log.info("Deleting workflow trigger with ID: {}", triggerId);
        WorkflowTrigger trigger = triggerRepository.findById(triggerId).orElseThrow(
                () -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString())
        );

        triggerRepository.deleteById(triggerId);

        if (trigger.getType() == TriggerType.SCHEDULE) {
            workflowSchedulerService.removeSchedule(triggerId);
        }
        log.info("Deleted workflow trigger with ID: {}", triggerId);
    }

    @Transactional
    public WorkflowTriggerDto enableTrigger(UUID triggerId) {
        log.info("Enabling workflow trigger with ID: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        trigger.setEnabled(true);
        trigger = triggerRepository.save(trigger);

        return triggerMapper.toDto(trigger);
    }

    @Transactional
    public WorkflowTriggerDto disableTrigger(UUID triggerId) {
        log.info("Disabling workflow trigger with ID: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        trigger.setEnabled(false);
        trigger = triggerRepository.save(trigger);

        return triggerMapper.toDto(trigger);
    }

    @Transactional
    public UUID executeTrigger(UUID triggerId, WorkflowRunnerRequest request) {
        UUID workflowRunId = UUID.randomUUID();
        log.info("Execute workflow trigger with ID: {}", triggerId);
        WorkflowTrigger trigger = markTriggered(triggerId);
        publisher.publishEvent(new WorkflowTriggerEvent(workflowRunId, trigger, request));
        return workflowRunId;
    }

    @Transactional
    public WorkflowTrigger markTriggered(UUID triggerId) {
        log.debug("Marking trigger as triggered: {}", triggerId);

        WorkflowTrigger trigger = triggerRepository.findById(triggerId)
                .orElseThrow(() -> new WorkflowTriggerException.WorkflowTriggerNotFound(triggerId.toString()));

        trigger.setLastTriggeredAt(OffsetDateTime.now());
        return triggerRepository.save(trigger);
    }

    private void validateTriggerConfiguration(CreateWorkflowTriggerRequest request) {
        validateTriggerConfigurationForType(request.getType(), request.getConfig());
    }

    private void validateTriggerConfigurationForType(TriggerType type, Map<String, Object> config) {
        switch (type) {
            case SCHEDULE:
                if (config == null || !config.containsKey("cron")) {
                    throw new WorkflowTriggerException.InvalidTriggerConfiguration("Schedule trigger requires 'cron' configuration");
                }
                break;
            case WEBHOOK:
                if (config == null || !config.containsKey("endpoint")) {
                    throw new WorkflowTriggerException.InvalidTriggerConfiguration("Webhook trigger requires 'endpoint' configuration");
                }
                break;
            case EVENT:
                if (config == null || !config.containsKey("eventType")) {
                    throw new WorkflowTriggerException.InvalidTriggerConfiguration("Event trigger requires 'eventType' configuration");
                }
                break;
            case MANUAL:
                // Manual triggers don't require specific configuration
                break;
            default:
                throw new WorkflowTriggerException.InvalidTriggerConfiguration("Unsupported trigger type: " + type);
        }
    }
}
