package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.context.ResolveConfigService;

@Slf4j
@Service
@AllArgsConstructor
public class TriggerOrchestrator {

    private final WorkflowTriggerRepository repo;
    private final Map<UUID, TriggerExecutor.RunningHandle> running = new ConcurrentHashMap<>();
    private final TriggerContext ctx;
    private final TriggerRegistry registry;
    private final WorkflowRepository workflowRepository;
    private final ResolveConfigService resolveConfigService;

    public void startAllEnabled() {
        repo.findByEnabledTrue().forEach(this::start);
    }

    public synchronized void start(WorkflowTrigger t) {
        if (running.containsKey(t.getId())) {
            log.debug("Trigger {} is already running", t.getId());
            return;
        }

        try {
            // Use the stored trigger_executor_id instead of building from TriggerType
            UUID triggerExecutorId = t.getTriggerExecutorId();
            if (triggerExecutorId == null) {
                log.warn("No trigger executor ID found for trigger {} of type {}", t.getId(), t.getType());
                return;
            }

            // Get trigger executor from registry using UUID (cast to string for flexibility)
            TriggerExecutor executor = registry.getRegistry(triggerExecutorId.toString());

            if (executor == null) {
                log.warn("No executor found for trigger executor ID: {}", triggerExecutorId);
                return;
            }

            // Handle resource management if the trigger needs it - use manual registration for persistence
            handleResourceRegistration(executor, t);

            // Resolve reserved config dynamically (secrets/profiles) while keeping templates in DB
            WorkflowTrigger effectiveTrigger = buildEffectiveTrigger(t);

            // Start the trigger
            var handle = executor.start(effectiveTrigger, ctx);
            running.put(t.getId(), handle);

            log.info("Started trigger {} (executor: {}) for workflow {}",
                    t.getId(), triggerExecutorId, t.getWorkflowId());

        } catch (Exception e) {
            log.error("Failed to start trigger {}: {}", t.getId(), e.getMessage(), e);
        }
    }

    private WorkflowTrigger buildEffectiveTrigger(WorkflowTrigger t) {
        try {
            if (t.getConfig() == null || t.getWorkflowId() == null || t.getTriggerExecutorId() == null) {
                return t; // nothing to resolve
            }

            // Find nodeKey from definition using triggerExecutorId (nodeId)
            String nodeKey = null;
            Workflow wf = workflowRepository.findById(t.getWorkflowId()).orElse(null);
            if (wf != null && wf.getDefinition() != null && wf.getDefinition().nodes() != null) {
                WorkflowDefinition def = wf.getDefinition();
                BaseWorkflowNode node = def.nodes().findByNodeId(t.getTriggerExecutorId());
                if (node != null) {
                    nodeKey = node.getKey();
                }
            }

            if (nodeKey == null) {
                log.debug("Node key not found for trigger {} (nodeId: {}). Using raw config.", t.getId(), t.getTriggerExecutorId());
                return t;
            }

            // Resolve reserved values using definition-phase resolver
            Map<String, Object> resolved = resolveConfigService
                    .resolveConfig(new WorkflowConfig(t.getConfig(), null, null), t.getWorkflowId(), nodeKey)
                    .input();

            // Create a detached copy with resolved config to avoid persisting values
            WorkflowTrigger copy = new WorkflowTrigger();
            copy.setId(t.getId());
            copy.setWorkflowId(t.getWorkflowId());
            copy.setType(t.getType());
            copy.setTriggerExecutorId(t.getTriggerExecutorId());
            copy.setEnabled(t.getEnabled());
            copy.setLastTriggeredAt(t.getLastTriggeredAt());
            copy.setCreatedAt(t.getCreatedAt());
            copy.setUpdatedAt(t.getUpdatedAt());
            copy.setConfig(resolved);
            return copy;
        } catch (Exception e) {
            log.warn("Failed to build effective trigger for {}. Using raw config.", t.getId(), e);
            return t;
        }
    }

    public synchronized void stop(UUID triggerId) {
        var handle = running.remove(triggerId);
        if (handle != null) {
            try {
                // Get the trigger from the database to handle resource cleanup
                repo.findById(triggerId).ifPresent(this::handleResourceUnregistration);

                handle.stop();
                log.info("Stopped trigger {}", triggerId);
            } catch (Exception e) {
                log.error("Error stopping trigger {}: {}", triggerId, e.getMessage(), e);
            }
        }
    }

    public synchronized void stop(WorkflowTrigger trigger) {
        var handle = running.remove(trigger.getId());
        if (handle != null) {
            try {
                handleResourceUnregistration(trigger);

                handle.stop();
                log.info("Stopped trigger {}:", trigger.getId());
            } catch (Exception e) {
                log.error("Error stopping trigger {}: {}", trigger.getId(), e.getMessage(), e);
            }
        }
    }

    public synchronized void restart(WorkflowTrigger t) {
        stop(t.getId());
        if (t.getEnabled()) start(t);
    }

    // Safety reconciler: ensure DB truth matches runtime
    public void reconcile() {
        var desired = new HashSet<UUID>();
        for (WorkflowTrigger t : repo.findByEnabledTrue()) {
            desired.add(t.getId());
            if (!running.containsKey(t.getId())) start(t);
        }
        // stop any extra
        for (UUID runningId : new ArrayList<>(running.keySet())) {
            if (!desired.contains(runningId)) stop(runningId);
        }
    }

    /**
     * Get status of all running triggers
     */
    public Map<UUID, String> getRunningStatus() {
        Map<UUID, String> status = new HashMap<>();
        running.forEach((id, handle) -> status.put(id, handle.getStatus()));
        return status;
    }


    private void handleResourceRegistration(TriggerExecutor executor, WorkflowTrigger trigger) {
        executor.getResourceManager().ifPresent(resourceManager ->
                executor.getResourceKey(trigger).ifPresent(resourceKey -> {
                    try {
                        resourceManager.registerNodeUsage(resourceKey, trigger.getId());
                        log.debug("Registered trigger {} for resource {})", trigger.getId(), resourceKey);
                    } catch (Exception e) {
                        log.error("Failed resource registration for trigger {}: {}", trigger.getId(), e.getMessage(), e);
                    }
                }));
    }

    private void handleResourceUnregistration(WorkflowTrigger trigger) {
        try {
            UUID triggerExecutorId = trigger.getTriggerExecutorId();
            if (triggerExecutorId == null) {
                log.debug("No executor ID for trigger {}, skipping resource cleanup", trigger.getId());
                return;
            }

            TriggerExecutor executor = registry.getRegistry(triggerExecutorId.toString());
            if (executor == null) return;

            executor.getResourceManager().ifPresent(resourceManager ->
                    executor.getResourceKey(trigger).ifPresent(resourceKey -> {
                        try {
                            resourceManager.unregisterNodeUsage(resourceKey, trigger.getId());
                            log.debug("Unregistered trigger {} from resource {}", trigger.getId(), resourceKey);
                        } catch (Exception e) {
                            log.error("Failed resource unregistration for trigger {}: {}", trigger.getId(), e.getMessage(), e);
                        }
                    }));
        } catch (Exception e) {
            log.error("Error in resource unregistration for trigger {}: {}", trigger.getId(), e.getMessage(), e);
        }
    }
}
