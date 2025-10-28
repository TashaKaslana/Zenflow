package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.manual;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContextTool;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@Slf4j
public class ManualTriggerExecutor implements TriggerExecutor {
    @Override
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.empty(); // Manual triggers don't need resource pooling
    }

    @Override
    public Optional<String> getResourceKey(TriggerContext trigger) {
        return Optional.empty(); // No resource key needed
    }

    @Override
    public RunningHandle start(TriggerContext triggerCtx, TriggerContextTool contextTool) throws Exception {
        WorkflowTrigger trigger = triggerCtx.trigger();
        log.info("Starting manual trigger for workflow: {}", trigger.getWorkflowId());

        // Manual triggers are essentially always "ready" but don't actively trigger
        // They wait for external manual execution calls
        log.info("Manual trigger registered and ready for workflow: {}", trigger.getWorkflowId());

        return new ManualRunningHandle(trigger.getId());
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Executing ManualTriggerExecutor with config: {}", context.getCurrentConfig());
        logs.info("Manual trigger started at {}", OffsetDateTime.now());

        Object payload = context.read("payload", Object.class);

        context.write("trigger_type", "manual");
        context.write("triggered_at", OffsetDateTime.now().toString());
        context.write("trigger_source", "manual_execution");

        if (payload != null) {
            context.write("payload", payload);
            logs.info("Payload received: {}", payload);
        } else {
            logs.info("No payload provided");
        }

        logs.success("Manual trigger completed successfully");
        return ExecutionResult.success();
    }

    /**
     * Running handle for manual triggers - they're always "ready" but passive
     */
    private static class ManualRunningHandle implements RunningHandle {
        private final UUID triggerId;
        private volatile boolean running = true;

        public ManualRunningHandle(UUID triggerId) {
            this.triggerId = triggerId;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;
                log.info("Manual trigger stopped: {}", triggerId);
            }
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public String getStatus() {
            return running ? "READY" : "STOPPED";
        }
    }
}