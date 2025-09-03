package org.phong.zenflow.workflow.subdomain.trigger.quartz;

import lombok.AllArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerContext;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quartz job that executes workflow triggers.
 * This follows the same pattern as your database connection pooling -
 * one job class, multiple instances with different configurations.
 */
@Component
@Slf4j
@AllArgsConstructor
public class WorkflowTriggerJob implements Job {

    private TriggerContext triggerContext;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            // Extract trigger information from job context
            var jobDataMap = context.getJobDetail().getJobDataMap();

            UUID triggerId = UUID.fromString(jobDataMap.getString("triggerId"));
            UUID workflowId = UUID.fromString(jobDataMap.getString("workflowId"));
            String description = jobDataMap.getString("description");
            Integer intervalSeconds = jobDataMap.getInt("intervalSeconds");
            String cronExpression = jobDataMap.getString("cronExpression");
            String triggerExecutorId = jobDataMap.getString("triggerExecutorId");

            log.debug("Quartz trigger firing for workflow: {} (trigger: {})", workflowId, triggerId);

            // Create payload with trigger information (supporting both interval and cron)
            Map<String, Object> payload = new HashMap<>();
            payload.put("trigger_type", "schedule");
            payload.put("triggered_at", OffsetDateTime.now().toString());
            payload.put("trigger_id", triggerId.toString());
            payload.put("job_key", context.getJobDetail().getKey().toString());
            payload.put("description", description);
            payload.put("scheduler", "quartz");

            // Add schedule-specific information based on type
            if (cronExpression != null && !cronExpression.trim().isEmpty()) {
                payload.put("cron_expression", cronExpression);
                payload.put("schedule_type", "cron");
                log.info("Quartz cron trigger fired: '{}' for workflow: {} (trigger: {})",
                        cronExpression, workflowId, triggerId);
            } else {
                payload.put("interval_seconds", intervalSeconds);
                payload.put("schedule_type", "interval");
                log.info("Quartz interval trigger fired: {}s for workflow: {} (trigger: {})",
                        intervalSeconds, workflowId, triggerId);
            }

            // Start the workflow
            triggerContext.startWorkflow(workflowId, UUID.fromString(triggerExecutorId), payload);
            triggerContext.markTriggered(triggerId, Instant.now());

        } catch (Exception e) {
            log.error("Error in Quartz job execution: {}", e.getMessage(), e);
            throw new JobExecutionException("Failed to execute workflow trigger", e);
        }
    }
}
