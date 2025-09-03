package org.phong.zenflow.workflow.subdomain.trigger.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.repository.WorkflowTriggerRepository;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.registry.TriggerRegistry;
import org.springframework.stereotype.Service;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class TriggerOrchestrator {

    private final WorkflowTriggerRepository repo;
    private final Map<UUID, TriggerExecutor.RunningHandle> running = new ConcurrentHashMap<>();
    private final TriggerContext ctx;
    private final TriggerRegistry registry;

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

            // Handle resource management if the trigger needs it
            executor.getResourceManager().ifPresentOrElse(resourceManager ->
                    executor.getResourceKey(t).ifPresent(resourceKey -> {
                        var resourceHandle = resourceManager.acquire(resourceKey, t.getId(), null);
                        try {
                            var delegate = executor.start(t, ctx);
                            running.put(t.getId(), new ResourceAwareRunningHandle(delegate, resourceHandle));
                            log.info("Started trigger {} (executor: {}) for workflow {}",
                                    t.getId(), triggerExecutorId, t.getWorkflowId());
                        } catch (Exception e) {
                            resourceHandle.close();
                            throw new RuntimeException(e);
                        }
                    }),
                    () -> {
                        try {
                            var handle = executor.start(t, ctx);
                            running.put(t.getId(), handle);
                            log.info("Started trigger {} (executor: {}) for workflow {}",
                                    t.getId(), triggerExecutorId, t.getWorkflowId());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

        } catch (Exception e) {
            log.error("Failed to start trigger {}: {}", t.getId(), e.getMessage(), e);
        }
    }

    public synchronized void stop(UUID triggerId) {
        var handle = running.remove(triggerId);
        if (handle != null) {
            try {
                // Get the trigger from database to handle resource cleanup
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


    private static class ResourceAwareRunningHandle implements TriggerExecutor.RunningHandle {
        private final TriggerExecutor.RunningHandle delegate;
        private final ScopedNodeResource<?> resourceHandle;

        ResourceAwareRunningHandle(TriggerExecutor.RunningHandle delegate, ScopedNodeResource<?> resourceHandle) {
            this.delegate = delegate;
            this.resourceHandle = resourceHandle;
        }

        @Override
        public void stop() {
            try (resourceHandle) {
                delegate.stop();
            }
        }

        @Override
        public String getStatus() {
            return delegate.getStatus();
        }

        @Override
        public boolean isRunning() {
            return delegate.isRunning();
        }
    }
}
