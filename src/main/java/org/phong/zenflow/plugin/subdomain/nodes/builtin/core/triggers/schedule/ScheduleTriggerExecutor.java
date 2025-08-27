package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.schedule;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.quartz.QuartzSchedulerResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.quartz.WorkflowTriggerJob;
import org.phong.zenflow.workflow.subdomain.trigger.resource.TriggerResourceManager;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.quartz.*;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@PluginNode(
        key = "core:schedule.trigger",
        name = "Schedule Trigger",
        version = "1.0.0",
        description = "Triggers a workflow based on a schedule using Quartz Scheduler. " +
                "Supports persistent scheduling with enterprise-level features.",
        type = "trigger",
        tags = {"core", "trigger", "schedule", "quartz"},
        icon = "ph:clock"
)
@Slf4j
@AllArgsConstructor
public class ScheduleTriggerExecutor implements TriggerExecutor {

    private final QuartzSchedulerResourceManager quartzResourceManager;

    @Override
    public String key() {
        return "core:schedule.trigger:1.0.0";
    }

    @Override
    public Optional<TriggerResourceManager<?>> getResourceManager() {
        return Optional.of(quartzResourceManager);
    }

    @Override
    public Optional<String> getResourceKey(WorkflowTrigger trigger) {
        // Use a shared scheduler resource key (could be tenant-based in the future)
        return Optional.of("default-scheduler");
    }

    @Override
    public RunningHandle start(WorkflowTrigger trigger, TriggerContext ctx) throws Exception {
        log.info("Starting Quartz schedule trigger for workflow: {}", trigger.getWorkflowId());

        Map<String, Object> config = trigger.getConfig();

        // Support both interval-based and cron-based scheduling
        Integer intervalSeconds = (Integer) config.get("interval_seconds");
        String cronExpression = (String) config.get("cron_expression");
        String description = (String) config.getOrDefault("description", "Scheduled trigger");

        // Validate that either interval or cron is provided (following your validation pattern)
        if (intervalSeconds == null && (cronExpression == null || cronExpression.trim().isEmpty())) {
            throw new IllegalArgumentException("Either 'interval_seconds' or 'cron_expression' must be provided in trigger config");
        }

        // Log configuration (similar to your DB connection logging)
        if (cronExpression != null && !cronExpression.trim().isEmpty()) {
            log.info("Quartz schedule trigger configured with cron: '{}', description: {}",
                     cronExpression, description);
        } else {
            log.info("Quartz schedule trigger configured: {} seconds interval, description: {}",
                     intervalSeconds, description);
        }

        // Create resource configuration following BaseDbConnection pattern
        String resourceKey = getResourceKey(trigger).orElse("default-scheduler");

        // For schedule triggers, we don't need a specific field from config as resource key
        // All schedule triggers can share the same scheduler, unlike DB connections which need different parameters
        ScheduleTriggerResourceConfig resourceConfig = new ScheduleTriggerResourceConfig(trigger);

        // Get or create shared Quartz Scheduler (like GlobalDbConnectionPool.getOrCreate())
        Scheduler scheduler = quartzResourceManager.getOrCreateResource(resourceKey, resourceConfig);

        // Create Quartz job (similar to creating a SQL statement)
        JobDetail job = JobBuilder.newJob(WorkflowTriggerJob.class)
                .withIdentity("trigger-" + trigger.getId(), "workflow-triggers")
                .usingJobData("triggerId", trigger.getId().toString())
                .usingJobData("workflowId", trigger.getWorkflowId().toString())
                .usingJobData("description", description)
                .usingJobData("intervalSeconds", intervalSeconds != null ? intervalSeconds : 0)
                .usingJobData("cronExpression", cronExpression != null ? cronExpression : "")
                .build();

        // Create Quartz trigger with appropriate schedule
        Trigger quartzTrigger;
        if (cronExpression != null && !cronExpression.trim().isEmpty()) {
            // Cron-based trigger
            quartzTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + trigger.getId(), "workflow-triggers")
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing()) // Handle misfires gracefully
                    .build();
        } else {
            // Interval-based trigger (existing logic)
            quartzTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + trigger.getId(), "workflow-triggers")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(intervalSeconds)
                            .repeatForever())
                    .build();
        }

        // Schedule the job using the shared scheduler
        quartzResourceManager.scheduleJob(resourceKey, job, quartzTrigger);

        log.info("Quartz schedule trigger started successfully for trigger: {}", trigger.getId());

        return new QuartzRunningHandle(resourceKey, quartzTrigger.getKey(), trigger.getId(),
                                     intervalSeconds, cronExpression, description, quartzResourceManager);
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        try {
            logs.info("Executing ScheduleTriggerExecutor with config: {}", config);
            logs.info("Schedule trigger started at {}", OffsetDateTime.now());

            // Extract optional payload and schedule configuration from input
            Map<String, Object> input = config.input();
            Object payload = input.get("payload");
            String cronExpression = (String) input.get("cron_expression");
            String scheduleDescription = (String) input.get("schedule_description");

            // Create output map with trigger metadata and payload
            Map<String, Object> output = new HashMap<>();
            output.put("trigger_type", "schedule");
            output.put("triggered_at", OffsetDateTime.now().toString());
            output.put("trigger_source", "scheduled_execution");
            output.put("scheduler", "quartz");

            // Add schedule-specific metadata
            if (cronExpression != null) {
                output.put("cron_expression", cronExpression);
                logs.info("Schedule triggered with cron: {}", cronExpression);
            }

            if (scheduleDescription != null) {
                output.put("schedule_description", scheduleDescription);
            }

            // Include payload in output if provided
            if (payload != null) {
                output.put("payload", payload);
                logs.info("Payload received: {}", payload);
            } else {
                logs.info("No payload provided");
            }

            // Add any additional input parameters to output for flexibility
            input.forEach((key, value) -> {
                if (!Set.of("payload", "cron_expression", "schedule_description").contains(key)) {
                    output.put("input_" + key, value);
                }
            });

            logs.success("Schedule trigger completed successfully");
            return ExecutionResult.success(output);
        } catch (Exception e) {
            logs.withException(e).error("Unexpected error occurred during schedule trigger execution: {}", e.getMessage());
            return ExecutionResult.error(e.getMessage());
        }
    }

    /**
     * Running handle for Quartz-based scheduled triggers.
     * Follows the same pattern as database connection cleanup.
     */
    private static class QuartzRunningHandle implements RunningHandle {
        private final String resourceKey;
        private final TriggerKey triggerKey;
        private final UUID triggerId;
        private final Integer intervalSeconds;
        private final String cronExpression;
        private final String description;
        private final QuartzSchedulerResourceManager resourceManager;
        private volatile boolean running = true;

        public QuartzRunningHandle(String resourceKey, TriggerKey triggerKey, UUID triggerId,
                                 Integer intervalSeconds, String cronExpression, String description,
                                 QuartzSchedulerResourceManager resourceManager) {
            this.resourceKey = resourceKey;
            this.triggerKey = triggerKey;
            this.triggerId = triggerId;
            this.intervalSeconds = intervalSeconds;
            this.cronExpression = cronExpression;
            this.description = description;
            this.resourceManager = resourceManager;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;

                // Unschedule from shared Quartz scheduler (like closing a DB connection)
                resourceManager.unscheduleJob(resourceKey, triggerKey);

                // Log appropriate message based on schedule type
                if (cronExpression != null && !cronExpression.trim().isEmpty()) {
                    log.info("Quartz schedule trigger stopped: {} (cron: '{}', description: {})",
                            triggerId, cronExpression, description);
                } else {
                    log.info("Quartz schedule trigger stopped: {} (interval: {}s, description: {})",
                            triggerId, intervalSeconds, description);
                }
            }
        }

        @Override
        public boolean isRunning() {
            return running && resourceManager.isResourceHealthy(resourceKey);
        }

        @Override
        public String getStatus() {
            if (!running) return "STOPPED";
            return resourceManager.isResourceHealthy(resourceKey) ? "RUNNING" : "UNHEALTHY";
        }
    }
}
