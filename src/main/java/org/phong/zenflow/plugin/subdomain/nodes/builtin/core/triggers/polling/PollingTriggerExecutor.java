package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.services.SharedQuartzSchedulerService;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.quartz.PollingTriggerJob;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.resource.PollingResponseCacheManager;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;
import org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity.WorkflowTrigger;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContextTool;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

@Component
@Slf4j
@AllArgsConstructor
public class PollingTriggerExecutor implements TriggerExecutor {
    private final SharedQuartzSchedulerService schedulerService;
    private final PollingResponseCacheManager cacheManager;

    @Override
    public Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.of(cacheManager);
    }

    @Override
    public Optional<String> getResourceKey(TriggerContext triggerCtx) {
        return Optional.of(triggerCtx.trigger().getId().toString());
    }

    @Override
    public RunningHandle start(TriggerContext triggerCtx, TriggerContextTool contextTool) throws Exception {
        WorkflowTrigger trigger = triggerCtx.trigger();
        log.info("Starting Quartz-based polling trigger for workflow: {}", trigger.getWorkflowId());

        Map<String, Object> config = trigger.getConfig();

        // Extract required configuration
        String url = (String) config.get("url");
        Integer intervalSeconds = (Integer) config.get("interval_seconds");
        String httpMethod = (String) config.getOrDefault("http_method", "GET");
        String changeDetectionStrategy = (String) config.getOrDefault("change_detection", "full_response");
        String jsonPath = (String) config.get("json_path");

        // Optional configuration
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) config.get("headers");
        Object requestBody = config.get("request_body");
        Boolean includeResponse = (Boolean) config.getOrDefault("include_response", true);
        Integer timeoutSeconds = (Integer) config.getOrDefault("timeout_seconds", 30);

        // Validate required parameters
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("url is required in polling trigger configuration");
        }
        if (intervalSeconds == null || intervalSeconds < 1) {
            throw new IllegalArgumentException("interval_seconds must be a positive integer");
        }

        log.info("Polling trigger configured with Quartz: URL={}, interval={}s, method={}, detection={}",
                url, intervalSeconds, httpMethod, changeDetectionStrategy);

        // Create Quartz job with all necessary data
        JobDetail job = JobBuilder.newJob(PollingTriggerJob.class)
                .withIdentity("polling-trigger-" + trigger.getId(), "polling-triggers")
                .usingJobData("triggerId", trigger.getId().toString())
                .usingJobData("workflowId", trigger.getWorkflowId().toString())
                .usingJobData("url", url)
                .usingJobData("httpMethod", httpMethod)
                .usingJobData("changeDetectionStrategy", changeDetectionStrategy)
                .usingJobData("jsonPath", jsonPath != null ? jsonPath : "")
                .usingJobData("timeoutSeconds", timeoutSeconds)
                .usingJobData("includeResponse", includeResponse)
                .usingJobData("triggerExecutorId", trigger.getTriggerExecutorId().toString())
                .build();

        // Add complex objects to job data map
        job.getJobDataMap().put("headers", headers);
        job.getJobDataMap().put("requestBody", requestBody);
        job.getJobDataMap().put("triggerContext", contextTool);

        // Create Quartz trigger for scheduling
        Trigger quartzTrigger = TriggerBuilder.newTrigger()
                .withIdentity("polling-trigger-" + trigger.getId(), "polling-triggers")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(intervalSeconds)
                        .repeatForever())
                .build();

        // Schedule on the shared Quartz scheduler
        schedulerService.scheduleJob(job, quartzTrigger);

        log.info("Quartz polling trigger started successfully for trigger: {}", trigger.getId());

        return new QuartzPollingRunningHandle(quartzTrigger.getKey(), trigger.getId(),
                                            url, intervalSeconds, httpMethod,
                                            changeDetectionStrategy, schedulerService);
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("Executing PollingTriggerExecutor with config: {}", config);

        Map<String, Object> input = config.input();
        Object responseData = input.get("response_data");
        Object previousData = input.get("previous_data");
        String pollingUrl = (String) input.get("polling_url");
        String changeType = (String) input.get("change_type");

        Map<String, Object> output = new HashMap<>();
        output.put("trigger_type", "polling");
        output.put("triggered_at", OffsetDateTime.now().toString());
        output.put("trigger_source", "polling_change_detected");
        output.put("scheduler_type", "quartz");

        if (pollingUrl != null) {
            output.put("polling_url", pollingUrl);
        }

        if (changeType != null) {
            output.put("change_type", changeType);
            logs.info("Change detected: {}", changeType);
        }

        if (responseData != null) {
            output.put("current_response", responseData);
        }

        if (previousData != null) {
            output.put("previous_response", previousData);
        }

        // Add any additional input parameters
        input.forEach((key, value) -> {
            if (!Set.of("response_data", "previous_data", "polling_url", "change_type").contains(key)) {
                output.put("input_" + key, value);
            }
        });

        logs.success("Polling trigger completed successfully using Quartz scheduler");
        return ExecutionResult.success(output);
    }

    /**
     * Running handle for Quartz-based polling triggers with generic resource management
     */
    private static class QuartzPollingRunningHandle implements RunningHandle {
        private final TriggerKey triggerKey;
        private final UUID triggerId;
        private final String url;
        private final Integer intervalSeconds;
        private final String httpMethod;
        private final String changeDetectionStrategy;
        private final SharedQuartzSchedulerService schedulerService;
        private volatile boolean running = true;

        public QuartzPollingRunningHandle(TriggerKey triggerKey, UUID triggerId, String url,
                                        Integer intervalSeconds, String httpMethod,
                                        String changeDetectionStrategy,
                                        SharedQuartzSchedulerService schedulerService) {
            this.triggerKey = triggerKey;
            this.triggerId = triggerId;
            this.url = url;
            this.intervalSeconds = intervalSeconds;
            this.httpMethod = httpMethod;
            this.changeDetectionStrategy = changeDetectionStrategy;
            this.schedulerService = schedulerService;
        }

        @Override
        public void stop() {
            if (running) {
                running = false;

                // Unschedule from Quartz
                schedulerService.unscheduleJob(triggerKey);

                log.info("Quartz polling trigger stopped: {} (URL: {}, interval: {}s, method: {}, detection: {})",
                        triggerId, url, intervalSeconds, httpMethod, changeDetectionStrategy);
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
