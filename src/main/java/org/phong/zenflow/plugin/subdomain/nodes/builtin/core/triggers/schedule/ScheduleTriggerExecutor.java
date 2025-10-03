package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.schedule;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContextTool;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.quartz.WorkflowTriggerJob;
import org.phong.zenflow.core.services.SharedQuartzSchedulerService;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.springframework.stereotype.Component;
import org.quartz.*;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@Slf4j
@AllArgsConstructor
public class ScheduleTriggerExecutor implements TriggerExecutor {

    private final SharedQuartzSchedulerService schedulerService;
    // Remove resource manager - we don't need resource pooling for schedulers
    @Override
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.empty(); // No resource pooling needed
    }

    @Override
    public Optional<String> getResourceKey(TriggerContext trigger) {
        return Optional.empty(); // No resource key needed
    }

    @Override
    public RunningHandle start(TriggerContext triggerCtx, TriggerContextTool contextTool) throws Exception {
        WorkflowTrigger trigger = triggerCtx.trigger();
        log.info("Starting optimized schedule trigger for workflow: {}", trigger.getWorkflowId());

        Map<String, Object> config = trigger.getConfig();

        // Support both interval-based and cron-based scheduling
        Integer intervalSeconds = (Integer) config.get("interval_seconds");
        String cronExpression = (String) config.get("cron_expression");
        String description = (String) config.getOrDefault("description", "Scheduled trigger");

        // Validate configuration
        if (intervalSeconds == null && (cronExpression == null || cronExpression.trim().isEmpty())) {
            throw new IllegalArgumentException("Either 'interval_seconds' or 'cron_expression' must be provided in trigger config");
        }

        // Log configuration
        if (cronExpression != null && !cronExpression.trim().isEmpty()) {
            log.info("Schedule trigger configured with cron: '{}', description: {}",
                     cronExpression, description);
        } else {
            log.info("Schedule trigger configured: {} seconds interval, description: {}",
                     intervalSeconds, description);
        }

        // Create Quartz job
        JobDetail job = JobBuilder.newJob(WorkflowTriggerJob.class)
                .withIdentity("trigger-" + trigger.getId(), "workflow-triggers")
                .usingJobData("triggerId", trigger.getId().toString())
                .usingJobData("workflowId", trigger.getWorkflowId().toString())
                .usingJobData("description", description)
                .usingJobData("intervalSeconds", intervalSeconds != null ? intervalSeconds : 0)
                .usingJobData("cronExpression", cronExpression != null ? cronExpression : "")
                .usingJobData("triggerExecutorId", trigger.getTriggerExecutorId().toString())
                .build();

        // Create appropriate trigger
        Trigger quartzTrigger;
        if (cronExpression != null && !cronExpression.trim().isEmpty()) {
            quartzTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + trigger.getId(), "workflow-triggers")
                    .startNow()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
        } else {
            quartzTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + trigger.getId(), "workflow-triggers")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(intervalSeconds)
                            .repeatForever())
                    .build();
        }

        // Schedule on the shared scheduler (much more efficient)
        schedulerService.scheduleJob(job, quartzTrigger);

        log.info("Schedule trigger started successfully for trigger: {}", trigger.getId());

        return new OptimizedRunningHandle(quartzTrigger.getKey(), trigger.getId(),
                                        intervalSeconds, cronExpression, description, schedulerService);
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Executing ScheduleTriggerExecutor with config: {}", config);
        logs.info("Schedule trigger started at {}", OffsetDateTime.now());

        Map<String, Object> input = config.input();
        Object payload = input.get("payload");
        String cronExpression = (String) input.get("cron_expression");
        String scheduleDescription = (String) input.get("schedule_description");

        Map<String, Object> output = new HashMap<>();
        output.put("trigger_type", "schedule");
        output.put("triggered_at", OffsetDateTime.now().toString());
        output.put("trigger_source", "scheduled_execution");
        output.put("scheduler", "shared-quartz");

        if (cronExpression != null) {
            output.put("cron_expression", cronExpression);
            logs.info("Schedule triggered with cron: {}", cronExpression);
        }

        if (scheduleDescription != null) {
            output.put("schedule_description", scheduleDescription);
        }

        if (payload != null) {
            output.put("payload", payload);
            logs.info("Payload received: {}", payload);
        } else {
            logs.info("No payload provided");
        }

        input.forEach((key, value) -> {
            if (!Set.of("payload", "cron_expression", "schedule_description").contains(key)) {
                output.put("input_" + key, value);
            }
        });

        logs.success("Schedule trigger completed successfully");
        return ExecutionResult.success(output);
    }

    /**
     * Optimized running handle that uses the shared scheduler.
     */
    private static class OptimizedRunningHandle implements RunningHandle {
        private final TriggerKey triggerKey;
        private final UUID triggerId;
        private final Integer intervalSeconds;
        private final String cronExpression;
        private final String description;
        private final SharedQuartzSchedulerService schedulerService;
        private volatile boolean running = true;

        public OptimizedRunningHandle(TriggerKey triggerKey, UUID triggerId,
                                    Integer intervalSeconds, String cronExpression, String description,
                                    SharedQuartzSchedulerService schedulerService) {
            this.triggerKey = triggerKey;
            this.triggerId = triggerId;
            this.intervalSeconds = intervalSeconds;
            this.cronExpression = cronExpression;
            this.description = description;
            this.schedulerService = schedulerService;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;
                schedulerService.unscheduleJob(triggerKey);

                if (cronExpression != null && !cronExpression.trim().isEmpty()) {
                    log.info("Schedule trigger stopped: {} (cron: '{}', description: {})",
                            triggerId, cronExpression, description);
                } else {
                    log.info("Schedule trigger stopped: {} (interval: {}s, description: {})",
                            triggerId, intervalSeconds, description);
                }
            }
        }

        @Override
        public boolean isRunning() {
            return running && schedulerService.isSchedulerHealthy();
        }

        @Override
        public String getStatus() {
            if (!running) return "STOPPED";
            return schedulerService.isSchedulerHealthy() ? "RUNNING" : "UNHEALTHY";
        }
    }
}
